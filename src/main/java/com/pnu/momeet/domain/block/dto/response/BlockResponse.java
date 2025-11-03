package com.pnu.momeet.domain.block.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BlockResponse(
    UUID blockerProfileId,
    UUID blockedProfileId,
    LocalDateTime createdAt
) {
}
