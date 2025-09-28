package com.pnu.momeet.domain.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    NOT_FOUND("NOT_FOUND", "채팅방 또는 참여자를 찾을 수 없습니다."),
    INVALID_MESSAGE("INVALID_MESSAGE_TYPE", "유효하지 않은 메시지 형식입니다."),
    SEND_FAILURE("MESSAGE_SEND_FAILURE", "메시지 전송에 실패했습니다."),
    UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS", "권한이 없는 접근입니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),
    INVALID_STATE("INVALID_STATE", "유효하지 않은 상태입니다.");
    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
