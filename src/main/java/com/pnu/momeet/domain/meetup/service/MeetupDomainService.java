package com.pnu.momeet.domain.meetup.service;

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
import com.pnu.momeet.domain.meetup.repository.spec.MeetupSpecifications;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupDtoMapper;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import java.time.Duration;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupDomainService {

    private static final long MIN_LEAD_MINUTES = 30; // 시작 최소 리드타임(분)

    private final GeometryFactory geometryFactory;
    private final MeetupEntityService entityService;
    private final ProfileEntityService profileService;
    private final SigunguEntityService sigunguService;

    private void validateTimeUnits(String startAt, String endAt) {

        try {
            LocalDateTime startTime = LocalDateTime.parse(startAt);
            LocalDateTime endTime = LocalDateTime.parse(endAt);

            if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
                throw new CustomValidationException(Map.of(
                    "timeUnit", List.of("종료 시간은 시작 시간 이후여야 합니다.")
                ));
            }

            long durationMin = Duration.between(startTime, endTime).toMinutes();
            if (durationMin % 10 != 0) {
                throw new CustomValidationException(Map.of(
                    "timeUnit", List.of("모임 지속 시간은 10분 단위여야 합니다.")
                ));
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime minStart = now.plusMinutes(MIN_LEAD_MINUTES);
            if (startTime.isBefore(minStart)) {
                throw new CustomValidationException(Map.of(
                    "timeUnit", List.of("시작 시간은 현재로부터 최소 " + MIN_LEAD_MINUTES + "분 이후여야 합니다.")
                ));
            }
        } catch (DateTimeParseException e) {
            throw new CustomValidationException(Map.of(
                "timeUnit", List.of("시간 형식이 올바르지 않습니다. yyyy-MM-ddTHH:mm 형식을 사용해주세요.")
            ));
        }

    }

    @Transactional(readOnly = true)
    public List<MeetupResponse> getAllByLocation(
        MeetupGeoSearchRequest request,
        UUID viewerMemberId
    ) {
        Point locationPoint = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
        MainCategory mainCategory = (request.category() != null) ? MainCategory.valueOf(request.category()) : null;
        String search = request.search();

        return entityService.getAllByLocationAndCondition(
            locationPoint, request.radius(), mainCategory, search, viewerMemberId
        )
            .stream()
            .map(MeetupEntityMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<MeetupResponse> getAllBySpecification(
        MeetupPageRequest request,
        UUID viewerMemberId
    ) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(request);
        Specification<Meetup> specification = MeetupDtoMapper.toSpecification(request)
            .and(MeetupSpecifications.visibleTo(viewerMemberId));

        Page<Meetup> page = entityService.getAllBySpecificationWithPagination(specification, pageRequest);

        return page.map(MeetupEntityMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MeetupDetail getById(UUID meetupId) {
        var meetup =  entityService.getByIdWithDetails(meetupId);
        return MeetupEntityMapper.toDetail(meetup);
    }

    @Transactional(readOnly = true)
    public MeetupDetail getById(UUID meetupId, UUID viewerMemberId) {
        if (entityService.isBlockedInMeetup(meetupId, viewerMemberId)) {
            log.info("차단 관계의 사용자가 있는 모임 조회 시도. id={}", meetupId);
            throw new NoSuchElementException("해당 Id의 모임이 존재하지 않습니다. id=" + meetupId);
        }
        var meetup =  entityService.getByIdWithDetails(meetupId);
        return MeetupEntityMapper.toDetail(meetup);
    }

    @Transactional(readOnly = true)
    public MeetupDetail getOwnedActiveMeetupByMemberID(UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        Meetup activeMeetups = entityService.getParticipatedMeetupByProfileId(profileId);
        return MeetupEntityMapper.toDetail(activeMeetups);
    }

    @Transactional(readOnly = true)
    public Page<Meetup> findEndedMeetupsByProfileId(UUID profileId, Pageable pageable) {
        return entityService.getEndedMeetupsByProfileId(profileId, pageable);
    }

    @Transactional
    public MeetupDetail createMeetup(MeetupCreateRequest request, UUID memberId) {
        validateTimeUnits(request.startAt(), request.endAt());

        UUID profileId = profileService.mapToProfileId(memberId);
        if (entityService.existsParticipatedMeetupByProfileId(profileId)) {
            log.info("이미 참여중인 모임이 있음. memberId={}", memberId);
            throw new CustomValidationException(Map.of(
                    "owner", List.of("이미 참여중인 모임이 있습니다. 모임이 종료된 후에 새 모임을 생성할 수 있습니다.")
            ));
        }
        Point locationPoint = geometryFactory.createPoint(new Coordinate(
                request.location().longitude(),
                request.location().latitude()
        ));

        Profile profile = profileService.getByMemberId(memberId);
        if (profile.getTemperature().doubleValue() < request.scoreLimit()) {
            log.info("회원님의 모임 참여 점수가 모임의 제한 점수보다 낮음. memberId={}, profileId={}, scoreLimit={}, temperature={}",
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
            log.info("수정 가능한 모임이 없음. memberId={}", memberId);
            throw new NoSuchElementException("수정 가능한 모임이 없습니다.");
        }
        Meetup meetup = meetups.getFirst();

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
}