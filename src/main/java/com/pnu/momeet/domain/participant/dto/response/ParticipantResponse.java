package com.pnu.momeet.domain.participant.dto.response;

import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;

import java.time.LocalDateTime;

public record ParticipantResponse(
        Long id,
        ProfileResponse profile,
        String role,
        Boolean isRated,
        LocalDateTime lastActiveAt,
        LocalDateTime createdAt
) {
}
