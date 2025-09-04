package com.pnu.momeet.domain.member.repository;

import com.pnu.momeet.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Page<Member> findAllBy(Pageable pageable);
    Optional<Member> findMemberByEmail(String email);
    boolean existsByEmail(String email);
}
