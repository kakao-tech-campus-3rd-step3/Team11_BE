package com.pnu.momeet.domain.meetup.dto.response;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetupDetail(
        UUID id,
        String name,
        String category,
        String description,
        Integer participantCount,
        Integer capacity,
        Double scoreLimit,
        ProfileResponse owner,
        SigunguResponse sigungu,
        LocationResponse location,
        List<String> hashTags,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
