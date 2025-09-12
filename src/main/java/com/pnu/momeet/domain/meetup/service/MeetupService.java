package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.common.exception.CustomValidationException;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupGeoSearchRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupDtoMapper;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class MeetupService {

    private final GeometryFactory geometryFactory;
    private final MeetupRepository meetupRepository;
    private final ProfileService profileService;
    private final SigunguService sigunguService;

    @Transactional(readOnly = true)
    public List<MeetupResponse> findAllByLocation(
            MeetupGeoSearchRequest request
    ) {
        Point location = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
        MainCategory mainCategory = request.category() != null ? MainCategory.valueOf(request.category()) : null;
        SubCategory subCategory = request.subCategory() != null ? SubCategory.valueOf(request.subCategory()) : null;

        if (mainCategory != null && subCategory != null) {
            if (subCategory.getMainCategory() != mainCategory) {
                return List.of(); // 카테고리 불일치 시 빈 리스트 반환
            }
        }
        List<Meetup> responses;

        if (subCategory != null && request.search() != null) {
            responses = meetupRepository.findAllByDistanceAndSubCategoryAndKeyword(
                    location, request.radius(), subCategory.name(), request.search());
        } else if (mainCategory != null && request.search() != null) {
            responses = meetupRepository.findAllByDistanceAndCategoryAndKeyword(
                    location, request.radius(), mainCategory.name(), request.search());
        } else if (subCategory != null) {
            responses = meetupRepository.findAllByDistanceAndSubCategory(
                    location, request.radius(), subCategory.name());
        } else if (mainCategory != null) {
            responses = meetupRepository.findAllByDistanceAndCategory(
                    location, request.radius(), mainCategory.name());
        } else if (request.search() != null) {
            responses = meetupRepository.findAllByDistanceAndKeyword(
                    location, request.radius(), request.search());
        } else {
            responses = meetupRepository.findAllByDistance(location, request.radius());
        }

        return responses.stream()
                .map(MeetupEntityMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MeetupResponse> findAllBySpecification(
            MeetupPageRequest request
    ) {
        PageRequest pageRequest = MeetupDtoMapper.toPageRequest(request);
        Specification<Meetup> specification = MeetupDtoMapper.toSpecification(request);

        return meetupRepository.findAll(specification, pageRequest)
                .map(MeetupEntityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Meetup findEntityById(UUID meetupId) {
        return meetupRepository.findById(meetupId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 모임이 존재하지 않습니다: " + meetupId));
    }

    @Transactional(readOnly = true)
    public MeetupResponse findById(UUID meetupId) {
        Meetup meetup = findEntityById(meetupId);
        return MeetupEntityMapper.toDto(meetup);
    }

    @Transactional(readOnly = true)
    public Meetup findEntityByMemberId(UUID memberId) {
        Profile profile = profileService.getProfileEntityByMemberId(memberId);
        return meetupRepository.findByOwner(profile)
                .orElseThrow(() -> new NoSuchElementException("해당 회원이 소유한 모임이 존재하지 않습니다: " + memberId));
    }

    @Transactional(readOnly = true)
    public MeetupResponse findByMemberId(UUID memberId) {
        Meetup meetup = findEntityByMemberId(memberId);
        return MeetupEntityMapper.toDto(meetup);
    }

    @Transactional
    public MeetupResponse createMeetup(MeetupCreateRequest request, UUID memberId) {
        Point locationPoint = geometryFactory.createPoint(new Coordinate(
                request.location().longitude(),
                request.location().latitude()
        ));
        Meetup meetup = MeetupDtoMapper.toEntity(request);
        Profile profile = profileService.getProfileEntityByMemberId(memberId);

        if (profile.getTemperature().doubleValue() < meetup.getScoreLimit()) {
            throw new CustomValidationException(Map.of(
                    "scoreLimit", List.of("회원님의 모임 참여 점수가 모임의 제한 점수보다 낮습니다.")
            ));
        }

        meetup.setOwner(profileService.getProfileEntityByMemberId(memberId));
        meetup.setLocationPoint(locationPoint);
        meetup.setSigungu(sigunguService.findEntityByPointIn(locationPoint));
        Meetup createdMeetup = meetupRepository.save(meetup);
        createdMeetup.setHashTags(request.hashTags()); // 해시태그는 모임 ID가 있어야 설정 가능
        return MeetupEntityMapper.toDto(createdMeetup);
    }

    @Transactional
    Meetup updateMeetupEntityById(UUID meetupId, Consumer<Meetup> updater) {
        Meetup meetup = findEntityById(meetupId);
        updater.accept(meetup);
        return meetup;
    }

    @Transactional
    public MeetupResponse updateMeetupByMemberId(MeetupUpdateRequest request, UUID memberId) {
        Meetup meetup = findEntityByMemberId(memberId);
        
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
        return MeetupEntityMapper.toDto(updatedMeetup);
    }

    @Transactional
    public void deleteMeetupAdmin(UUID meetupId) {
        Meetup meetup = findEntityById(meetupId);
        meetupRepository.delete(meetup);
    }

    @Transactional
    public void deleteMeetup(UUID memberId) {
        Meetup meetup = findEntityByMemberId(memberId);
        if (meetup.getStatus() == MeetupStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 모임은 삭제할 수 없습니다.");
        }
        meetupRepository.delete(meetup);
    }
}