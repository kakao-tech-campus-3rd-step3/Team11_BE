package com.pnu.momeet.domain.member.mapper;

import com.pnu.momeet.domain.member.dto.MemberInfo;
import com.pnu.momeet.domain.member.dto.MemberResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.entity.MemberRole;

import java.util.List;

public class EntityMapper {

    private EntityMapper() {
        // 이 클래스의 인스턴스화 방지
    }

    private static List<String> extractRoleNames(List<MemberRole> roles) {
        return roles.stream()
                .map(MemberRole::getName)
                .map(Enum::name)
                .toList();
    }

    public static MemberInfo toMemberInfo(Member member) {
        return new MemberInfo(
                member.getId(),
                member.getEmail(),
                member.getProvider(),
                member.getPassword(),
                member.getProviderId(),
                extractRoleNames(member.getRoles()),
                member.isEnabled(),
                member.isAccountNonLocked(),
                member.getTokenIssuedAt()
        );
    }

    public static MemberResponse toDto(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getProvider(),
                extractRoleNames(member.getRoles()),
                member.isEnabled(),
                member.isAccountNonLocked(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
