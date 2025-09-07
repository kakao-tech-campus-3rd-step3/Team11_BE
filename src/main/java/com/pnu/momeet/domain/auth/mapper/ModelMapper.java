package com.pnu.momeet.domain.auth.mapper;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.domain.auth.dto.TokenResponse;

public class ModelMapper {
    private ModelMapper() {
        // private constructor to prevent instantiation
    }

    public static TokenResponse toDto(TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
