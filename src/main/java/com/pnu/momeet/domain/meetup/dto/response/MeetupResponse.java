package com.pnu.momeet.domain.meetup.dto.response;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetupResponse(
    UUID id,
    UUID ownerProfileId,
    Long sigunguCode,
    String name,
    String category,
    String subCategory,
    List<String> hashTags,
    Integer participantCount,
    Integer capacity,
    Double scoreLimit,
    LocationResponse location,
    String status,
    LocalDateTime endAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}