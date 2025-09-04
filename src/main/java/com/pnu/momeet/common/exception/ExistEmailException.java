package com.pnu.momeet.common.exception;

public class ExistEmailException extends RuntimeException {
    public ExistEmailException(String msg) {
        super(msg);
    }
}
