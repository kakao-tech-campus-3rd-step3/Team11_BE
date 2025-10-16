package com.pnu.momeet.domain.member.service.mapper;

import com.pnu.momeet.domain.common.mapper.PageMapper;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.request.MemberEditRequest;
import com.pnu.momeet.domain.member.dto.request.MemberPageRequest;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MemberDtoMapper {
    private MemberDtoMapper() {
        // private constructor to prevent instantiation
    }

    public static Consumer<Member> toConsumer(MemberEditRequest request) {
        return member -> {
            if (request.roles() != null && !request.roles().isEmpty()) {
                member.setRoles( request.roles()
                        .stream()
                        .map(Role::valueOf)
                        .collect(Collectors.toSet())
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
                        .collect(Collectors.toSet()),
                true // 관리자 생성은 모두 인증된 상태로
        );
    }

    public static PageRequest toPageRequest(MemberPageRequest request) {
        if (request.getSort() == null || request.getSort().isEmpty()) {
            return PageRequest.of(request.getPage(), request.getSize());
        }
        Sort sort = PageMapper.toSort(request.getSort());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
