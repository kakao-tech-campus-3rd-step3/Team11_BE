package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.exception.CustomValidationException;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupGeoSearchRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupDtoMapper;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupDomainService {

    private final GeometryFactory geometryFactory;
    private final MeetupEntityService entityService;
    private final ProfileEntityService profileService;
    private final SigunguEntityService sigunguService;
    private final ParticipantEntityService participantService;
    private final CoreEventPublisher coreEventPublisher;

    private void validateCategories(String mainCategory, String subCategory) {
        if (mainCategory == null || subCategory == null) {
            return;
        }
        if (!SubCategory.valueOf(subCategory).getMainCategory().name().equals(mainCategory)) {
            throw new CustomValidationException(Map.of(
                "subCategory", List.of("서브 카테고리가 메인 카테고리에 속하지 않습니다.")
            ));
        }
    }
    @Transactional(readOnly = true)
    public List<MeetupResponse> getAllByLocation(
            MeetupGeoSearchRequest request
    ) {
        Point locationPoint = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
        Optional<MainCategory> mainCategory = Optional.ofNullable(request.category()).map(MainCategory::valueOf);
        Optional<SubCategory> subCategory = Optional.ofNullable(request.subCategory()).map(SubCategory::valueOf);
        Optional<String> search = Optional.ofNullable(request.search());

        return entityService.getAllByLocationAndCondition(
            locationPoint, request.radius(), mainCategory, subCategory, search
        )
            .stream()
            .map(MeetupEntityMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<MeetupResponse> getAllBySpecification(
            MeetupPageRequest request
    ) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(request);
        Specification<Meetup> specification = MeetupDtoMapper.toSpecification(request);

        return entityService.getAllBySpecificationWithPagination(specification, pageRequest)
                .map(MeetupEntityMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MeetupDetail getById(UUID meetupId) {
        var meetup =  entityService.getByIdWithDetails(meetupId);
        return MeetupEntityMapper.toDetail(meetup);
    }

    @Transactional(readOnly = true)
    public MeetupDetail getOwnedActiveMeetupByMemberID(UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        List<Meetup> activeMeetups = entityService.getAllByOwnerIdAndStatusIn(
                profileId, List.of(MeetupStatus.OPEN, MeetupStatus.IN_PROGRESS)
        );
        if (activeMeetups.isEmpty()) {
            log.warn("진행 중이거나 모집 중인 모임이 없음. memberId={}", memberId);
            throw new NoSuchElementException("진행 중이거나 모집 중인 모임이 없습니다.");
        }
        return MeetupEntityMapper.toDetail(activeMeetups.getFirst());
    }

    @Transactional(readOnly = true)
    public Page<Meetup> findEndedMeetupsByProfileId(UUID profileId, Pageable pageable) {
        return entityService.getEndedMeetupsByProfileId(profileId, pageable);
    }

    @Transactional
    public MeetupDetail createMeetup(MeetupCreateRequest request, UUID memberId) {
        validateCategories(request.category(), request.subCategory());
        if (entityService.existsByOwnerIdAndStatusIn(
                profileService.mapToProfileId(memberId),
                List.of(MeetupStatus.OPEN, MeetupStatus.IN_PROGRESS)
        )) {
            log.warn("이미 진행 중이거나 모집 중인 모임이 있음. memberId={}", memberId);
            throw new CustomValidationException(Map.of(
                    "owner", List.of("이미 진행 중이거나 모집 중인 모임이 있습니다. 하나의 모임만 생성할 수 있습니다.")
            ));
        }
        Point locationPoint = geometryFactory.createPoint(new Coordinate(
                request.location().longitude(),
                request.location().latitude()
        ));

        Profile profile = profileService.getByMemberId(memberId);
        if (profile.getTemperature().doubleValue() < request.scoreLimit()) {
            log.warn("회원님의 모임 참여 점수가 모임의 제한 점수보다 낮음. memberId={}, profileId={}, scoreLimit={}, temperature={}",
                    memberId, profile.getId(), request.scoreLimit(), profile.getTemperature());
            throw new CustomValidationException(Map.of(
                    "scoreLimit", List.of("회원님의 모임 참여 점수가 모임의 제한 점수보다 낮습니다.")
            ));
        }
        Sigungu sigungu = sigunguService.getByPointIn(locationPoint);
        Meetup meetup = MeetupDtoMapper.toEntity(request, locationPoint,  profile, sigungu);
        meetup = entityService.createMeetup(meetup, request.hashTags());
        meetup.addParticipant(MeetupEntityMapper.toHostParticipant(meetup));

        log.info("모임 생성 성공. id={}, ownerId={}", meetup.getId(), profile.getId());
        // fetch join을 사용하기 위해 다시 조회
        return getById(meetup.getId());
    }

    @Transactional
    public MeetupDetail updateMeetupByMemberId(MeetupUpdateRequest request, UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        var meetups = entityService.getAllByOwnerIdAndStatusIn(profileId, List.of(MeetupStatus.OPEN));
        if (meetups.isEmpty()) {
            log.warn("수정 가능한 모임이 없음. memberId={}", memberId);
            throw new NoSuchElementException("수정 가능한 모임이 없습니다.");
        }
        Meetup meetup = meetups.getFirst();
        validateCategories(request.category(), request.subCategory());

        Meetup updatedMeetup = entityService.updateMeetup(
                meetup, MeetupDtoMapper.toConsumer(request)
        );
        // 영속성 처리
        if (request.location() != null) {
            Point locationPoint = geometryFactory.createPoint(new Coordinate(
                    request.location().longitude(),
                    request.location().latitude()
            ));
            log.info("모임 위치 변경 감지. id={}, ownerId={}", updatedMeetup.getId(), profileId);
            updatedMeetup.setLocationPoint(locationPoint);
            updatedMeetup.setSigungu(sigunguService.getByPointIn(locationPoint));
        }
        // fetch join을 사용하기 위해 다시 조회
        log.info("모임 수정 성공. id={}, ownerId={}", updatedMeetup.getId(), profileId);
        return getById(updatedMeetup.getId());
    }

    @Transactional
    public void deleteMeetupAdmin(UUID meetupId) {
        entityService.deleteById(meetupId);
        log.info("관리자에 의해 모임 삭제 성공. id={}", meetupId);
    }

    @Transactional
    public void deleteMeetup(UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        var meetups = entityService.getAllByOwnerIdAndStatusIn(profileId, List.of(MeetupStatus.OPEN));
        if (meetups.isEmpty()) {
            log.warn("삭제 가능한 모임이 없음. memberId={}", memberId);
            throw new NoSuchElementException("삭제 가능한 모임이 없습니다.");
        }
        Meetup meetup = meetups.getFirst();
        entityService.deleteById(meetup.getId());
        log.info("사용자 본인의 모임 삭제 성공. id={}, ownerId={}", meetup.getId(), profileId);
    }

    @Transactional
    public void finishMeetupByMemberId(UUID memberId, UUID meetupId) {
        // 1) 소유자 검증: memberId -> profileId -> 소유한 IN_PROGRESS 모임 중 해당 id 확인
        UUID ownerProfileId = profileService.mapToProfileId(memberId);
        Meetup meetup = entityService.getById(meetupId);
        if (!meetup.getOwner().getId().equals(ownerProfileId)) {
            throw new IllegalStateException("방장만 종료할 수 있습니다.");
        }
        if (meetup.getStatus() != MeetupStatus.IN_PROGRESS) {
            throw new IllegalStateException("종료할 수 있는 상태가 아닙니다.");
        }

        // 2) 상태 전이: ENDED (엔티티 메서드로 캡슐화 권장)
        entityService.updateMeetup(meetup, m -> {
            m.setStatus(MeetupStatus.ENDED);
            m.setEndAt(LocalDateTime.now());
        });

        // 3) 완주자 조회 -> 프로필 집계 필드 증가
        var participants = participantService.getAllByMeetupId(meetupId);
        List<UUID> finisherIds = new ArrayList<>();
        for (var p : participants) {
            UUID profileId = p.getProfile().getId();
            Profile profile = profileService.getById(profileId);
            profile.increaseCompletedJoinMeetups();
            finisherIds.add(profileId);
        }

        // 4) 종료 이벤트 발행 (커밋 후 배지 부여)
        coreEventPublisher.publish(
            new MeetupFinishedEvent(
                meetupId,
                participants.stream().map(p -> p.getProfile().getId()).toList()
            ));

        log.info("모임 종료 완료. meetupId={}, ownerProfileId={}, finisherCount={}",
            meetupId, ownerProfileId, finisherIds.size());
    }
}