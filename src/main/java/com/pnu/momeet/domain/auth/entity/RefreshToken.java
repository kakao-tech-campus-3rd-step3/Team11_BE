package com.pnu.momeet.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    private UUID memberId;
    @Column(name = "token_value", nullable = false, length = 512)
    private String value;

    public RefreshToken(UUID memberId, String value) {
        this.memberId = memberId;
        this.value = value;
    }
}
