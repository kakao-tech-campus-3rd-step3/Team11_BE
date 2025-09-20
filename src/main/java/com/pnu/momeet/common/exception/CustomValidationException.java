package com.pnu.momeet.common.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CustomValidationException extends RuntimeException {
    Map<String, List<String>> fieldErrors;

    public CustomValidationException(Map<String, List<String>> fieldErrors) {
        super("유효성 검사에 실패했습니다.");
        this.fieldErrors = fieldErrors;
    }
}

