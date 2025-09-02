package com.pnu.momeet.domain.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class RefreshToken {
    @Id
    private UUID memberId;
    @Column(name = "token_value", nullable = false)
    private String value;

    protected RefreshToken() {

    }

    public RefreshToken(UUID memberId, String value) {
        this.memberId = memberId;
        this.value = value;
    }
}
