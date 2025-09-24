package com.pnu.momeet.domain.evaluation.service;

import com.pnu.momeet.common.mapper.facade.EvaluatableProfileMapper;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.evaluation.service.mapper.EvaluationEntityMapper;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationCommandService {

    private final EvaluationRepository evaluationRepository;
    private final ProfileDomainService profileService;
    private final ParticipantDomainService participantService;

    private static final Duration EVALUATION_COOLTIME = Duration.ofHours(24);

    @Transactional
    public EvaluationResponse createEvaluation(
        UUID evaluatorMemberId,
        EvaluationCreateRequest request,
        String ipHash
    ) {
        Profile evaluatorProfile = profileService.getProfileEntityByMemberId(evaluatorMemberId);
        Profile targetProfile = profileService.getProfileEntityByProfileId(request.targetProfileId());

        // 모임 단위 중복 평가 방지
        if (evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            request.meetupId(), evaluatorProfile.getId(), targetProfile.getId())) {
            throw new IllegalStateException("이미 평가한 사용자입니다.");
        }

        // 동일 evaluator → target 에 대한 쿨타임 검증
        evaluationRepository.findTopByEvaluatorProfileIdAndTargetProfileIdOrderByCreatedAtDesc(
                evaluatorProfile.getId(),
                targetProfile.getId()
            ).filter(lastEval -> lastEval
                .getCreatedAt()
                .isAfter(LocalDateTime.now().minus(EVALUATION_COOLTIME)))
            .ifPresent(e -> {
                throw new IllegalStateException("동일 사용자에 대한 평가는 하루에 한 번만 가능합니다.");
            });

        // 동일 위치 ( IP 해시 ) 다계정 평가 검증
        boolean existsBySameIp = evaluationRepository
            .existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
                request.meetupId(),
                targetProfile.getId(),
                ipHash,
                LocalDateTime.now().minus(EVALUATION_COOLTIME)
            );
        if (existsBySameIp) {
            throw new IllegalStateException("동일 위치에서 이미 해당 사용자에 대한 평가가 등록되었습니다.");
        }

        switch (request.rating()) {
            case LIKE:
                targetProfile.increaseLikes();
                break;
            case DISLIKE:
                targetProfile.increaseDislikes();
                break;
            default:
                throw new IllegalArgumentException("유효하지 않은 평가입니다.");
        }

        Evaluation newEvaluation = Evaluation.create(
            request.meetupId(),
            evaluatorProfile.getId(),
            targetProfile.getId(),
            request.rating(),
            ipHash
        );

        return EvaluationEntityMapper.toResponseDto(evaluationRepository.save(newEvaluation));
    }

    @Transactional(readOnly = true)
    public long calculateUnEvaluatedCount(Meetup meetup, UUID evaluatorProfileId) {
        long evaluatedCount = evaluationRepository.countByMeetupIdAndEvaluatorProfileId(
            meetup.getId(), evaluatorProfileId
        );

        return meetup.getParticipantCount() - 1 - evaluatedCount;
    }

    @Transactional(readOnly = true)
    public List<EvaluatableProfileResponse> getEvaluatableUsers(UUID meetupId, UUID evaluatorProfileId) {
        // 1. 모임 참가자 조회
        List<ParticipantResponse> participants = participantService.getParticipantsByMeetupId(meetupId);

        // 2. 자기 자신 제외
        List<ParticipantResponse> targetParticipants = participants.stream()
            .filter(p -> !p.profile().id().equals(evaluatorProfileId))
            .toList();

        // 3. 기존 평가 기록 불러오기
        Map<UUID, Evaluation> existingEvaluations = evaluationRepository
            .findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfileId)
            .stream()
            .collect(Collectors.toMap(Evaluation::getTargetProfileId, e -> e));

        // 4. 응답 변환
        return targetParticipants.stream()
            .map(participantResponse -> EvaluatableProfileMapper
                .toEvaluatableProfileResponse(
                    participantResponse,
                    existingEvaluations.get(participantResponse.profile().id())
                ))
            .toList();
    }
}
