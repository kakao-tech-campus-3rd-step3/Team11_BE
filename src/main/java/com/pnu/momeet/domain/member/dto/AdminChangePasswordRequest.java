package com.pnu.momeet.domain.member.dto;

import com.pnu.momeet.common.validation.annotation.Password;
import jakarta.validation.constraints.NotBlank;

public record AdminChangePasswordRequest(
        @NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
        @Password
        String newPassword
) {
}
