package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.domain.meetup.dto.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetupService {

    private final MeetupRepository meetupRepository;
    private final ProfileRepository profileRepository;

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
