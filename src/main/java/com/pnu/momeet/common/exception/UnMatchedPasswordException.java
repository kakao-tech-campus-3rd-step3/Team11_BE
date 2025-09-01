package com.pnu.momeet.common.exception;

public class UnMatchedPasswordException extends RuntimeException {
    public UnMatchedPasswordException(String message) {
        super(message);
    }
}
