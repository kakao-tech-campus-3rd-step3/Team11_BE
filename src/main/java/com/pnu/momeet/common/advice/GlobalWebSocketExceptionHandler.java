package com.pnu.momeet.common.advice;

import com.pnu.momeet.common.exception.MessageSendFailureException;
import com.pnu.momeet.domain.common.dto.response.CustomErrorResponse;
import com.pnu.momeet.domain.common.enums.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@ControllerAdvice(basePackages = "com.pnu.momeet.domain.chatting")
@Order(1)
public class GlobalWebSocketExceptionHandler {

    @MessageExceptionHandler(NoSuchElementException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleNoSuchElementException(NoSuchElementException exception) {
        ErrorCode code = ErrorCode.NOT_FOUND;

        return new CustomErrorResponse(
            code.getCode(),
            code.getMessage(),
            exception.getMessage(),
            LocalDateTime.now()
        );
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorCode code = ErrorCode.INVALID_MESSAGE;

        return new CustomErrorResponse(
            code.getCode(),
            code.getMessage(),
            exception.getMessage(),
            LocalDateTime.now()
        );
    }

    @MessageExceptionHandler(MessageSendFailureException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleMessageSendFailureException(MessageSendFailureException exception) {
        ErrorCode code = ErrorCode.SEND_FAILURE;

        return new CustomErrorResponse(
            code.getCode(),
            code.getMessage(),
            exception.getMessage(),
            LocalDateTime.now()
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleException(Exception exception) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR;

        return new CustomErrorResponse(
            code.getCode(),
            code.getMessage(),
            exception.getMessage(),
            LocalDateTime.now()
        );
    }
}
