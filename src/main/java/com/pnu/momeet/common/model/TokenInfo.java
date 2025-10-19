package com.pnu.momeet.common.model;

import com.pnu.momeet.common.model.enums.TokenType;

import java.time.LocalDateTime;

public record TokenInfo(
        String subject,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        TokenType tokenType
) {
}
