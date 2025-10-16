package com.pnu.momeet.domain.member.repository;

import com.pnu.momeet.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findMemberByEmail(String email);
    boolean existsByEmail(String email);
    long deleteAllByVerifiedIsFalseAndCreatedAtBefore(LocalDateTime dateTime);
}
