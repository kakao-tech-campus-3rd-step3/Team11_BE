package com.pnu.momeet.domain.auth.repository;

import com.pnu.momeet.domain.auth.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findByMemberId(UUID memberId);
}
