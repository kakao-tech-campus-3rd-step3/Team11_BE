package com.pnu.momeet.domain.badge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BadgeAwardRequest(

    @NotBlank(message = "배지 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "배지 코드는 대문자/숫자/밑줄만 가능합니다.")
    String code
) {
}
