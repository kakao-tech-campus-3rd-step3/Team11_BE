package com.pnu.momeet.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pnu.momeet.domain.common.enums.Provider;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberInfo(
        UUID id,
        String email,
        Provider provider,
        String password,
        String providerId,
        List<String> roles,
        boolean enabled,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        boolean accountNonLocked
) {

}
