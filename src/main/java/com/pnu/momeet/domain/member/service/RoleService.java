package com.pnu.momeet.domain.member.service;

import com.pnu.momeet.domain.common.enums.MemberRole;
import com.pnu.momeet.domain.member.entity.Role;
import com.pnu.momeet.domain.member.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    private MemberRole parseRoleName(String name) {
        try {
            return MemberRole.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 역할 이름입니다. name=" + name);
        }
    }

    public Role findRoleByName(String name) {
        return roleRepository.findByName(parseRoleName(name)).orElseThrow(
            () -> new IllegalArgumentException("유효하지 않은 역할 이름입니다. name=" + name)
        );
    }

    public List<Role> findRolesByNames(List<String> names) {
        List<MemberRole> roleNames = names.stream()
            .map(this::parseRoleName)
            .toList();
        return roleRepository.findByNameIn(roleNames);
    }
}
