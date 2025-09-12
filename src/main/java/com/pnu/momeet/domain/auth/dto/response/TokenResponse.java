package com.pnu.momeet.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
