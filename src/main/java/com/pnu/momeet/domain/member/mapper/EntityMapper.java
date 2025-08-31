package com.pnu.momeet.domain.member.mapper;

import com.pnu.momeet.domain.member.dto.MemberInfo;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.entity.Role;

import java.util.List;

public class EntityMapper {

    private EntityMapper() {
        // 이 클래스의 인스턴스화 방지
    }

    public static MemberInfo toMemberInfo(Member member) {
        List<String> roleNames = member.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .toList();

        return new MemberInfo(
                member.getId(),
                member.getEmail(),
                member.getProvider(),
                member.getPassword(),
                member.getProviderId(),
                roleNames,
                member.isEnabled(),
                member.isAccountNonExpired(),
                member.isCredentialsNonExpired(),
                member.isAccountNonLocked()
        );
    }
}
