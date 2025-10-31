package com.pnu.momeet.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoCallbackRequest(
        @NotBlank(message = "인증 코드는 필수입니다.")
        String code,
        
        @NotBlank(message = "redirect_uri는 필수입니다.")
        String redirectUri
) {
}
