package com.pnu.momeet.domain.evaluation.dto.response;

import java.util.UUID;

public record InvalidItem(
    UUID targetProfileId,
    String message
) {
}
