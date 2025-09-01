package com.pnu.momeet.domain.member.dto;

import com.pnu.momeet.common.validation.annotation.Password;
import com.pnu.momeet.common.validation.annotation.RoleSet;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MemberCreateRequest(
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Password
        String password,

        @NotEmpty(message = "역할은 최소 한 개 이상 선택해야 합니다.")
        @RoleSet
        List<String> roles
) {
}
