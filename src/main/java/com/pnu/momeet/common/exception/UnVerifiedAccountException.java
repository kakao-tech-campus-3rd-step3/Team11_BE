package com.pnu.momeet.common.exception;

import org.springframework.security.core.AuthenticationException;

public class UnVerifiedAccountException extends AuthenticationException {
    public UnVerifiedAccountException(String msg) {
        super(msg);
    }
}
