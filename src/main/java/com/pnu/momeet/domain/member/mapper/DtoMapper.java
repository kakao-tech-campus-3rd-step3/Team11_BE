package com.pnu.momeet.domain.member.mapper;

import com.pnu.momeet.domain.member.dto.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.MemberEditRequest;
import com.pnu.momeet.domain.member.dto.MemberPageRequest;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DtoMapper {
    private DtoMapper() {
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
                        .collect(Collectors.toSet())
        );
    }

    public static PageRequest toPageRequest(MemberPageRequest request) {
        if (request.getSort() == null || request.getSort().isEmpty()) {
            return PageRequest.of(request.getPage(), request.getSize());
        }

        String[] sortParams = request.getSort().split(",");
        List<Sort.Order> orders = new ArrayList<>();

        for (int i = 0; i < sortParams.length / 2; i++) {
            String field = sortParams[2 * i].trim();
            String direction = sortParams[2 * i + 1].trim().toLowerCase();
            var dir = Sort.Direction.valueOf(direction.toUpperCase());
            orders.add(new Sort.Order(dir, field));
        }

        Sort sort = Sort.by(orders);
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
