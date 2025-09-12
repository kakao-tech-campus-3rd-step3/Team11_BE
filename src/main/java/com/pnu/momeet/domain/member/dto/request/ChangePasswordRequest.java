package com.pnu.momeet.domain.member.dto.request;

import com.pnu.momeet.common.validation.annotation.Password;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "기존 비밀번호는 필수 입력 값입니다.")
        @Password
        String oldPassword,

        @NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
        @Password
        String newPassword
) {
}
