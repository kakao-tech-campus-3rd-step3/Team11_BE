package com.pnu.momeet.domain.auth.dto.request;

public record KakaoCallbackRequest(
        String code,
        String error,
        String error_description
) {
}
