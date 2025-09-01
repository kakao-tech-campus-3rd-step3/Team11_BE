package com.pnu.momeet.common.exception;

import org.springframework.security.core.AuthenticationException;

public class ConcurrentLoginException  extends AuthenticationException {
    public ConcurrentLoginException(String msg) {
        super(msg);
    }
}
