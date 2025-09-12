package com.pnu.momeet.domain.meetup.dto.response;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeetupResponse(
    UUID id,
    String name,
    String category,
    String subCategory,
    String description,
    Integer capacity,
    Integer scoreLimit,
    SigunguResponse sigungu,
    LocationResponse location,
    String status,
    LocalDateTime startAt,
    LocalDateTime endAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}