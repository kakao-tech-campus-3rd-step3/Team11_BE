package com.pnu.momeet.domain.profile.dto.response;

import com.pnu.momeet.domain.profile.enums.Gender;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileResponse(
    UUID id,
    String nickname,
    Integer age,
    Gender gender,
    String imageUrl,
    String description,
    String baseLocation,
    BigDecimal temperature,
    int likes,
    int dislikes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
