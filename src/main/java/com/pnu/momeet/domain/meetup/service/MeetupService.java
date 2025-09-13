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
import com.pnu.momeet.domain.meetup.repository.MeetupDslRepository;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupDtoMapper;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class MeetupService {

    private final GeometryFactory geometryFactory;

    private final MeetupRepository meetupRepository;
    private final MeetupDslRepository meetupDslRepository;

    private final ProfileService profileService;
    private final SigunguService sigunguService;

    private void validateCategories(String mainCategory, String subCategory) {
        if (mainCategory != null && subCategory != null) {
            if (!SubCategory.valueOf(subCategory).getMainCategory().name().equals(mainCategory)) {
                throw new CustomValidationException(Map.of(
                        "subCategory", List.of("서브 카테고리가 메인 카테고리에 속하지 않습니다.")
                ));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<MeetupResponse> findAllByLocation(
            MeetupGeoSearchRequest request
    ) {
        return meetupDslRepository.findAllByDistanceAndPredicates(
            geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude())),
            request.radius(),
            Optional.ofNullable(request.category()).map(MainCategory::valueOf),
            Optional.ofNullable(request.subCategory()).map(SubCategory::valueOf),
            Optional.ofNullable(request.search())
        )
            .stream()
            .map(MeetupEntityMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<MeetupResponse> findAllBySpecification(
            MeetupPageRequest request
    ) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(request);
        Specification<Meetup> specification = MeetupDtoMapper.toSpecification(request);

        return meetupRepository.findAll(specification, pageRequest)
                .map(MeetupEntityMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Meetup findEntityById(UUID meetupId) {
        return meetupRepository.findById(meetupId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 모임이 존재하지 않습니다: " + meetupId));
    }

    @Transactional(readOnly = true)
    public Meetup findOwnedEntityByMemberId(UUID memberId) {
        return meetupDslRepository.findOwnedMeetupByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 회원이 소유한 모임이 존재하지 않습니다: " + memberId));
    }

    @Transactional(readOnly = true)
    public MeetupDetail findById(UUID meetupId) {
        return meetupDslRepository.findByIdWithDetails(meetupId)
                .map(MeetupEntityMapper::toDetail)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 모임이 존재하지 않습니다: " + meetupId));
    }

    @Transactional(readOnly = true)
    public MeetupDetail findOwnedMeetupByMemberID(UUID memberId) {
        return meetupDslRepository.findOwnedMeetupByMemberIdWithDetails(memberId)
                .map(MeetupEntityMapper::toDetail)
                .orElseThrow(() -> new NoSuchElementException("해당 회원이 소유한 모임이 존재하지 않습니다: " + memberId));
    }

    @Transactional(readOnly = true)
    public boolean existsById(UUID meetupId) {
        return meetupRepository.existsById(meetupId);
    }

    @Transactional
    public MeetupDetail createMeetup(MeetupCreateRequest request, UUID memberId) {
        validateCategories(request.category(), request.subCategory());

        if (meetupDslRepository.existsByOwnerIdAndStatusIn(memberId, List.of(MeetupStatus.OPEN, MeetupStatus.IN_PROGRESS))) {
            throw new CustomValidationException(Map.of(
                    "owner", List.of("이미 진행 중이거나 모집 중인 모임이 있습니다. 하나의 모임만 생성할 수 있습니다.")
            ));
        }
        Point locationPoint = geometryFactory.createPoint(new Coordinate(
                request.location().longitude(),
                request.location().latitude()
        ));
        Meetup meetup = MeetupDtoMapper.toEntity(request);
        Profile profile = profileService.getProfileEntityByMemberId(memberId);
        Sigungu sigungu = sigunguService.findEntityByPointIn(locationPoint);

        if (profile.getTemperature().doubleValue() < meetup.getScoreLimit()) {
            throw new CustomValidationException(Map.of(
                    "scoreLimit", List.of("회원님의 모임 참여 점수가 모임의 제한 점수보다 낮습니다.")
            ));
        }
        meetup.setOwner(profile);
        meetup.setLocationPoint(locationPoint);
        meetup.setSigungu(sigungu);
        Meetup createdMeetup = meetupRepository.save(meetup);

        // 영속 이후에 해시태그 설정 및 호스트 참가자 추가
        createdMeetup.setHashTags(request.hashTags());
        
        Participant hostParticipant = Participant.builder()
                .meetup(createdMeetup)
                .profile(profile)
                .role(MeetupRole.HOST)
                .isActive(false)
                .isRated(false)
                .lastActiveAt(LocalDateTime.now())
                .build();
        createdMeetup.addParticipant(hostParticipant);
        // fetch join을 사용하기 위해 다시 조회
        return findById(createdMeetup.getId());
    }

    @Transactional
    public Meetup updateMeetupEntityById(UUID meetupId, Consumer<Meetup> updater) {
        Meetup meetup = findEntityById(meetupId);
        updater.accept(meetup);
        return meetup;
    }

    @Transactional
    public MeetupDetail updateMeetupByMemberId(MeetupUpdateRequest request, UUID memberId) {
        Meetup meetup = findOwnedEntityByMemberId(memberId);
        validateCategories(request.category(), request.subCategory());
        
        if (!meetup.getOwner().getMemberId().equals(memberId)) {
            throw new SecurityException("모임을 수정할 권한이 없습니다.");
        }
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            throw new IllegalStateException("모집 중인 모임만 수정할 수 있습니다.");
        }

        Meetup updatedMeetup = updateMeetupEntityById(meetup.getId(), MeetupDtoMapper.toConsumer(request));
        // 영속성 처리
        if (request.location() != null) {
            Point locationPoint = geometryFactory.createPoint(new Coordinate(
                    request.location().longitude(),
                    request.location().latitude()
            ));
            updatedMeetup.setLocationPoint(locationPoint);
            updatedMeetup.setSigungu(sigunguService.findEntityByPointIn(locationPoint));
        }

        // fetch join을 사용하기 위해 다시 조회
        return findById(updatedMeetup.getId());
    }

    @Transactional
    public void deleteMeetupAdmin(UUID meetupId) {
        Meetup meetup = findEntityById(meetupId);
        meetupRepository.delete(meetup);
    }

    @Transactional
    public void deleteMeetup(UUID memberId) {
        Meetup meetup = findOwnedEntityByMemberId(memberId);
        if (meetup.getStatus() == MeetupStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 모임은 삭제할 수 없습니다.");
        }
        meetupRepository.delete(meetup);
    }
}