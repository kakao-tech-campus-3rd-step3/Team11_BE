package com.pnu.momeet.common.exception;

import org.springframework.security.core.AuthenticationException;

public class DisabledAccountException extends AuthenticationException {
    public DisabledAccountException(String msg) {
        super(msg);
    }
}
