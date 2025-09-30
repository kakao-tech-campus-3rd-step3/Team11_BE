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
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupDtoMapper;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
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
            log.info("진행 중이거나 모집 중인 모임이 없음. memberId={}", memberId);
            throw new NoSuchElementException("진행 중이거나 모집 중인 모임이 없습니다.");
        }
        if (activeMeetups.size() > 1) {
            // 비정상적인 상황
            log.warn("진행 중이거나 모집 중인 모임이 2개 이상임. memberId={}, count={}", memberId, activeMeetups.size());
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
            log.info("이미 진행 중이거나 모집 중인 모임이 있음. memberId={}", memberId);
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
}