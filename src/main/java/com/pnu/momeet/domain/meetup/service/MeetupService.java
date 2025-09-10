package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.MeetupResponse;
import com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetupService {

    private final MeetupRepository meetupRepository;
    private final ProfileRepository profileRepository;
    private final MemberRepository memberRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public List<MeetupResponse> getAllMeetups(String category, String status, String search) {
        List<Meetup> meetups;

        // 기본 조회 (필터 없음)
        if (category == null && status == null && search == null) {
            meetups = meetupRepository.findAll();
        }
        // 검색어만 있는 경우
        else if (search != null) {
            meetups = meetupRepository.findBySearchKeyword(search, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        // 카테고리만 있는 경우
        else if (category != null && status == null) {
            meetups = meetupRepository.findByCategory(
                com.pnu.momeet.domain.meetup.enums.MeetupCategory.valueOf(category), 
                org.springframework.data.domain.Pageable.unpaged()
            ).getContent();
        }
        // 상태만 있는 경우
        else if (status != null && category == null) {
            meetups = meetupRepository.findByStatus(
                com.pnu.momeet.domain.meetup.enums.MeetupStatus.valueOf(status), 
                org.springframework.data.domain.Pageable.unpaged()
            ).getContent();
        }
        // 복합 필터
        else {
            meetups = meetupRepository.findByFilters(
                category, status, search, null, null, null, 
                org.springframework.data.domain.Pageable.unpaged()
            ).getContent();
        }

        return meetups.stream()
                .map(this::convertToMeetupResponse)
                .toList();
    }

    @Transactional
    public MeetupResponse createMeetup(UUID memberId, MeetupCreateRequest request) {
        if (request.getStartAt().isAfter(request.getEndAt())) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다");
        }

        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다"));

        // 위치 정보 변환 (DTO -> PostGIS Point)
        Point locationPoint = geometryFactory.createPoint(
                new Coordinate(request.getLocation().getLongitude(), request.getLocation().getLatitude())
        );
        locationPoint.setSRID(4326);

        Meetup meetup = new Meetup(
                owner,
                request.getName(),
                request.getCategory(),
                request.getDescription(),
                request.getTags(),
                request.getHashTags(),
                request.getCapacity() != null ? request.getCapacity() : 10,
                request.getScoreLimit(),
                locationPoint,
                request.getLocation().getAddress(),
                request.getStartAt(),
                request.getEndAt()
        );

        Meetup savedMeetup = meetupRepository.save(meetup);

        return convertToMeetupResponse(savedMeetup);
    }

    @Transactional
    public MeetupResponse updateMeetup(UUID meetupId, UUID memberId, MeetupUpdateRequest request) {
        if (request.getStartAt().isAfter(request.getEndAt())) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다");
        }

        // 모임 조회 및 소유자 권한 확인
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다"));

        if (!meetup.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("모임을 수정할 권한이 없습니다");
        }

        // 위치 정보 변환 (DTO -> PostGIS Point)
        Point locationPoint = geometryFactory.createPoint(
                new Coordinate(request.getLocation().getLongitude(), request.getLocation().getLatitude())
        );
        locationPoint.setSRID(4326);

        meetup.updateMeetup(
                request.getName(),
                request.getCategory(),
                request.getDescription(),
                request.getTags(),
                request.getHashTags(),
                request.getCapacity(),
                request.getScoreLimit(),
                locationPoint,
                request.getLocation().getAddress(),
                request.getStatus(),
                request.getStartAt(),
                request.getEndAt()
        );

        return convertToMeetupResponse(meetup);
    }

    private MeetupResponse convertToMeetupResponse(Meetup meetup) {
        // 위치 정보 변환 (PostGIS Point -> DTO)
        var location = new MeetupResponse.Location(
                meetup.getLocationPoint().getY(), // latitude
                meetup.getLocationPoint().getX(), // longitude
                meetup.getLocationText()
        );

        var ownerProfile = profileRepository.findByMemberId(meetup.getOwner().getId())
                .map(profile -> new MeetupResponse.OwnerProfile(
                        profile.getId(),
                        profile.getNickname(),
                        profile.getImageUrl(),
                        profile.getTemperature().doubleValue()
                ))
                .orElse(new MeetupResponse.OwnerProfile(
                        meetup.getOwner().getId(),
                        "프로필 없음",
                        null,
                        36.5
                ));

        return new MeetupResponse(
                meetup.getId(),
                meetup.getOwner().getId(),
                ownerProfile,
                meetup.getName(),
                meetup.getCategory(),
                meetup.getDescription(),
                meetup.getTags(),
                meetup.getHashTags(),
                meetup.getScoreLimit(),
                meetup.getStartAt(),
                meetup.getEndAt(),
                meetup.getStatus(),
                location,
                0, // TODO: Participant 엔티티 구현 후 참가자 수 수정
                meetup.getCapacity(),
                meetup.getCreatedAt(),
                meetup.getUpdatedAt()
        );
    }
}
