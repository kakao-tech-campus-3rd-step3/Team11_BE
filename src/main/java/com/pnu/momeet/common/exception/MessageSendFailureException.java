package com.pnu.momeet.common.exception;

public class MessageSendFailureException extends RuntimeException {
    public MessageSendFailureException(String message) {
        super(message);
    }
}
