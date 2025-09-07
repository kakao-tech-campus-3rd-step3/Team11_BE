package com.pnu.momeet.domain.auth.dto;

import com.pnu.momeet.common.validation.annotation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Password
        String password1,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String password2
) {
}
