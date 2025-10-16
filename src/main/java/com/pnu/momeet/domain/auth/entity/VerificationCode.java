package com.pnu.momeet.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member_verification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCode {
    private static final long EXPIRATION_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private UUID code;

    @Column(name = "member_id", nullable = false, unique = true)
    private UUID memberId;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public VerificationCode(UUID memberId) {
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusMinutes(EXPIRATION_MINUTES);
    }

    public VerificationCode renewCode() {
        this.code = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusMinutes(EXPIRATION_MINUTES);
        return this;
    }
}
