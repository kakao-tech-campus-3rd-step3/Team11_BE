package com.pnu.momeet.domain.member.dto;

import com.pnu.momeet.domain.member.enums.Provider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String email,
        Provider provider,
        List<String> roles,
        boolean enabled,
        boolean isAccountNonLocked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
