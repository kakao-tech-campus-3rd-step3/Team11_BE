package com.pnu.momeet.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
