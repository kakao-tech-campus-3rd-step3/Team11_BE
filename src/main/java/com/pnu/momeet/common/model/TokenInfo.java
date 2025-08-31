package com.pnu.momeet.common.model;

import java.time.LocalDateTime;

public record TokenInfo(
        String subject,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {
}
