package com.pnu.momeet.common.exception;

import org.springframework.security.core.AuthenticationException;

public class BannedAccountException extends AuthenticationException {
    public BannedAccountException(String msg) {
        super(msg);
    }
}
