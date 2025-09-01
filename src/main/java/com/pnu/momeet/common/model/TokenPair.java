package com.pnu.momeet.common.model;

public record TokenPair(
        String refreshToken,
        String accessToken
) {
}
