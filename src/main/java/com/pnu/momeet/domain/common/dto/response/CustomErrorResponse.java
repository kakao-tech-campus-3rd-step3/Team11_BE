package com.pnu.momeet.domain.common.dto.response;

import com.pnu.momeet.domain.common.enums.ErrorCode;

import java.time.LocalDateTime;

public record CustomErrorResponse(
        String errorCode,
        String message,
        String detail,
        LocalDateTime timestamp
) {

    public static CustomErrorResponse from(Exception ex, ErrorCode code) {
        return new CustomErrorResponse(
                code.getCode(),
                code.getMessage(),
                ex.getMessage(),
                LocalDateTime.now()
        );
    }
}
