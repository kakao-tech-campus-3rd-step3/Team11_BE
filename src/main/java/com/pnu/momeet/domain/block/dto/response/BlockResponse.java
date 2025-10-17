package com.pnu.momeet.domain.block.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BlockResponse(
    UUID blockerId,
    UUID blockedId,
    LocalDateTime createdAt
) {
}
