package com.pnu.momeet.domain.member.mapper;

import com.pnu.momeet.domain.member.dto.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.MemberEditRequest;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;

import java.util.function.Consumer;

public class DtoMapper {
    private DtoMapper() {
        // private constructor to prevent instantiation
    }

    public static Consumer<Member> toConsumer(MemberEditRequest request) {
        return member -> {
            if (request.roles() != null) {
                member.setRoles( request.roles()
                        .stream()
                        .map(Role::valueOf)
                        .toList()
                );
            }
            if (request.enabled() != null) {
                member.setEnabled(request.enabled());
            }

            if (request.accountNonLocked() != null) {
                member.setAccountNonLocked(request.accountNonLocked());
            }
        };
    }

    public static Member toEntity(MemberCreateRequest request) {
        return new Member(
                request.email(),
                request.password(),
                request.roles()
                        .stream()
                        .map(Role::valueOf)
                        .toList()
        );
    }
}
