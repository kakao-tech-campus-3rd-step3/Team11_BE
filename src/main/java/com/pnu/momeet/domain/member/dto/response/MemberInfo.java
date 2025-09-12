package com.pnu.momeet.domain.member.dto.response;

import com.pnu.momeet.domain.member.enums.Provider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MemberInfo(
        UUID id,
        String email,
        Provider provider,
        String password,
        String providerId,
        List<String> roles,
        boolean enabled,
        boolean isAccountNonLocked,
        LocalDateTime tokenIssuedAt
) {

}
