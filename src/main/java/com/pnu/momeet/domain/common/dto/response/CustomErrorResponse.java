package com.pnu.momeet.domain.common.dto.response;

import java.time.LocalDateTime;

public record CustomErrorResponse(
        String errorCode,
        String message,
        String detail,
        LocalDateTime timestamp
) {
}
