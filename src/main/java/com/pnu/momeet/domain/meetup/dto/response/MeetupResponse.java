package com.pnu.momeet.domain.meetup.dto.response;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetupResponse(
    UUID id,
    String name,
    String category,
    String subCategory,
    List<String> hashTags,
    String description,
    Integer capacity,
    Double scoreLimit,
    SigunguResponse sigungu,
    LocationResponse location,
    String status,
    LocalDateTime endAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}