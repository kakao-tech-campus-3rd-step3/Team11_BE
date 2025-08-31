package com.pnu.momeet.domain.member.repository;

import com.pnu.momeet.domain.common.enums.MemberRole;
import com.pnu.momeet.domain.member.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(MemberRole name);
    List<Role> findByNameIn(List<MemberRole> names);
}
