package com.pnu.momeet.domain.evaluation.service;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.mapper.facade.EvaluatableProfileMapper;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.evaluation.service.mapper.EvaluationEntityMapper;
import com.pnu.momeet.domain.meetup.dto.request.MeetupSummaryPageRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupSummaryResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupDtoMapper;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationDomainService {

    private static final Duration EVALUATION_COOLTIME = Duration.ofHours(24);

    private final EvaluationEntityService entityService;
    private final ProfileEntityService profileService;
    private final ParticipantEntityService participantService;
    private final MeetupEntityService meetupService;
    private final CoreEventPublisher coreEventPublisher;

    @Transactional
    public EvaluationResponse createEvaluation(UUID evaluatorMemberId,
        EvaluationCreateRequest request,
        String ipHash) {
        Profile evaluator = profileService.getByMemberId(evaluatorMemberId);
        Profile target = profileService.getById(request.targetProfileId());

        // 1. 자기 자신 평가 금지
        if (evaluator.getId().equals(target.getId())) {
            log.warn("자기 자신 평가 금지 위반. evaluatorPid={}, meetupId={}", evaluator.getId(), request.meetupId());
            throw new IllegalArgumentException("자기 자신은 평가할 수 없습니다.");
        }

        // 2. 모임 참가자 검증 (둘 다 해당 모임 참여자여야 함)
        if (!participantService.existsByProfileIdAndMeetupId(evaluator.getId(), request.meetupId()) &&
            !participantService.existsByProfileIdAndMeetupId(target.getId(), request.meetupId())
        ) {
            log.info("모임 참가자 검증 실패. meetupId={}, evaluatorPid={}, targetPid={}",
                request.meetupId(), evaluator.getId(), target.getId()
            );
            throw new IllegalStateException("모임에 참여하지 않은 사용자입니다.");
        }

        // 3. 중복/쿨타임/IP 검증
        if (entityService.existsByMeetupAndPair(request.meetupId(), evaluator.getId(), target.getId())) {
            log.warn("모임 단위 중복 평가. meetupId={}, evaluatorPid={}, targetPid={}",
                request.meetupId(), evaluator.getId(), target.getId());
            throw new IllegalStateException("이미 평가한 사용자입니다.");
        }
        entityService.getLastByPair(evaluator.getId(), target.getId())
            .filter(last -> last.getCreatedAt().isAfter(LocalDateTime.now().minus(EVALUATION_COOLTIME)))
            .ifPresent(x -> {
                log.warn("동일 evaluator → target 쿨타임 위반. evaluatorPid={}, targetPid={}",
                    evaluator.getId(), target.getId());
                throw new IllegalStateException("동일 사용자에 대한 평가는 하루에 한 번만 가능합니다.");
            });
        if (entityService.existsByMeetupTargetIpAfter(
            request.meetupId(), target.getId(), ipHash, LocalDateTime.now().minus(EVALUATION_COOLTIME))) {
            log.warn("동일 IP 해시 쿨타임 위반. meetupId={}, targetPid={}", request.meetupId(), target.getId());
            throw new IllegalStateException("동일 위치에서 이미 해당 사용자에 대한 평가가 등록되었습니다.");
        }

        // 4. 프로필 집계 반영
        switch (request.rating()) {
            case LIKE -> {
                target.increaseLikes();
                log.debug("target likes 증가. targetPid={}, likes={}", target.getId(), target.getLikes());
            }
            case DISLIKE -> {
                target.increaseDislikes();
                log.debug("target dislikes 증가. targetPid={}, dislikes={}", target.getId(), target.getDislikes());
            }
            default -> {
                log.warn("유효하지 않은 평가 타입. rating={}", request.rating());
                throw new IllegalArgumentException("유효하지 않은 평가입니다.");
            }
        }

        // 5. 저장
        Evaluation saved = entityService.save(Evaluation.create(
            request.meetupId(), evaluator.getId(), target.getId(), request.rating(), ipHash
        ));

        // 6. 이벤트 발행
        coreEventPublisher.publish(new EvaluationSubmittedEvent(
            request.meetupId(), evaluator.getId(), target.getId(), request.rating()
        ));
        log.info("평가 생성 완료. id={}, meetupId={}, evaluatorPid={}, targetPid={}, rating={}",
            saved.getId(), saved.getMeetupId(), saved.getEvaluatorProfileId(), saved.getTargetProfileId(), saved.getRating());

        return EvaluationEntityMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<MeetupSummaryResponse> getMyRecentMeetupsMixed(UUID memberId, MeetupSummaryPageRequest req) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(req);
        Profile me = profileService.getByMemberId(memberId);

        log.debug("최근 종료 모임(혼합) 조회 시도. profileId={}, page={}, size={}",
            me.getId(), pageRequest.getPageNumber(), pageRequest.getPageSize());

        // 1. 평가 조건 없이 최근 종료 모임(내가 참가) 페이지 조회
        Page<Meetup> meetups = meetupService.getEndedMeetupsByProfileId(me.getId(), pageRequest);

        // 2. 이번 페이지의 meetupId들에 대해 “내가 1건 이상 평가했는지” 배치 조회(Set)
        List<UUID> meetupIds = meetups.getContent().stream().map(Meetup::getId).toList();
        log.debug("혼합 조회 페이지 로드 완료. profileId={}, pageElements={}, collectingIds={}",
            me.getId(), meetups.getNumberOfElements(), meetupIds.size());

        Set<UUID> doneIds = entityService.getMeetupIdsEvaluatedBy(me.getId(), meetupIds);
        log.debug("평가 여부 배치 판정 완료. profileId={}, evaluatedMeetups={}", me.getId(), doneIds.size());

        // 3. DTO 매핑 시 evaluated 채우기
        List<MeetupSummaryResponse> content = meetups.getContent().stream()
            .map(m -> MeetupEntityMapper.toMeetupSummaryResponse(m, doneIds.contains(m.getId())))
            .toList();
        log.debug("최근 종료 모임(혼합) 조회 성공. profileId={}, elements={}, total={}",
            me.getId(), content.size(), meetups.getTotalElements());

        return new PageImpl<>(content, pageRequest, meetups.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<MeetupSummaryResponse> getMyEvaluatedMeetups(
        UUID memberId, MeetupSummaryPageRequest request
    ) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(request);
        Profile me = profileService.getByMemberId(memberId);

        log.debug("최근 종료 모임(평가함) 조회 시도. profileId={}, page={}, size={}",
            me.getId(), pageRequest.getPageNumber(), pageRequest.getPageSize());

        Page<Meetup> meetups = meetupService
            .getEndedMeetupsByProfileIdAndEvaluated(me.getId(), true, pageRequest);

        List<MeetupSummaryResponse> content = meetups.getContent().stream()
            .map(m -> MeetupEntityMapper.toMeetupSummaryResponse(m, true))
            .toList();

        log.debug("최근 종료 모임(평가함) 조회 성공. profileId={}, elements={}, total={}",
            me.getId(), content.size(), meetups.getTotalElements());

        return new PageImpl<>(content, pageRequest, meetups.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<MeetupSummaryResponse> getMyUnEvaluatedMeetups(
        UUID memberId, MeetupSummaryPageRequest request
    ) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(request);
        Profile me = profileService.getByMemberId(memberId);

        log.debug("최근 종료 모임(미평가) 조회 시도. profileId={}, page={}, size={}",
            me.getId(), pageRequest.getPageNumber(), pageRequest.getPageSize());

        Page<Meetup> meetups = meetupService
            .getEndedMeetupsByProfileIdAndEvaluated(me.getId(), false, pageRequest);

        List<MeetupSummaryResponse> content = meetups.getContent().stream()
            .map(m -> MeetupEntityMapper.toMeetupSummaryResponse(m, false))
            .toList();

        log.debug("최근 종료 모임(미평가) 조회 성공. profileId={}, elements={}, total={}",
            me.getId(), content.size(), meetups.getTotalElements());

        return new PageImpl<>(content, pageRequest, meetups.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<EvaluatableProfileResponse> getEvaluatableUsers(UUID meetupId, UUID evaluatorMemberId) {
        Profile evaluator = profileService.getByMemberId(evaluatorMemberId);

        // 1) 모임 참가자 & 자기 자신 제외
        List<Participant> participants = participantService.getAllByMeetupId(meetupId);
        List<Participant> targets = participants.stream()
            .filter(p -> !p.getProfile().getId().equals(evaluator.getId()))
            .toList();

        // 2) 기존 평가 맵
        Map<UUID, Evaluation> existing = entityService
            .getByMeetupAndEvaluator(meetupId, evaluator.getId())
            .stream()
            .collect(Collectors.toMap(Evaluation::getTargetProfileId, e -> e));

        // 3) 응답
        List<EvaluatableProfileResponse> result = targets.stream()
            .map(p -> EvaluatableProfileMapper.toEvaluatableProfileResponse(p, existing.get(p.getProfile().getId())))
            .toList();

        log.debug("평가 가능 사용자 조회 완료. meetupId={}, evaluatorPid={}, candidates={}",
            meetupId, evaluator.getId(), result.size());
        return result;
    }
}
