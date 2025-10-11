package com.pnu.momeet.common.exception;

import org.springframework.security.core.AuthenticationException;

public class SessionMismatchException extends AuthenticationException {
    
    public SessionMismatchException(String msg) {
        super(msg);
    }
}
