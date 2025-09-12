package com.pnu.momeet.domain.member.dto.request;

import com.pnu.momeet.common.validation.annotation.RoleSet;

import java.util.List;

public record MemberEditRequest(
        @RoleSet
        List<String> roles,
        Boolean enabled,
        Boolean accountNonLocked
) {

}
