package com.pnu.momeet.common.exception;

public class MailSendFailureException extends  RuntimeException {
    public MailSendFailureException(String message) {
        super(message);
    }
}
