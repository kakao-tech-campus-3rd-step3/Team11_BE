package com.pnu.momeet.common.advice;

import com.pnu.momeet.common.exception.MessageSendFailureException;
import com.pnu.momeet.domain.common.dto.response.CustomErrorResponse;
import com.pnu.momeet.domain.common.enums.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.NoSuchElementException;

@ControllerAdvice(basePackages = "com.pnu.momeet.domain.chatting")
@Order(1)
public class GlobalWebSocketExceptionHandler {

    @MessageExceptionHandler(NoSuchElementException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleNoSuchElementException(NoSuchElementException exception) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        return CustomErrorResponse.from(exception, code);
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorCode code = ErrorCode.INVALID_MESSAGE;
        return CustomErrorResponse.from(exception, code);
    }

    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleIllegalStateException(IllegalStateException exception) {
        ErrorCode code = ErrorCode.INVALID_STATE;
        return CustomErrorResponse.from(exception, code);
    }

    @MessageExceptionHandler(MessageSendFailureException.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleMessageSendFailureException(MessageSendFailureException exception) {
        ErrorCode code = ErrorCode.SEND_FAILURE;
        return CustomErrorResponse.from(exception, code);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public CustomErrorResponse handleException(Exception exception) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return CustomErrorResponse.from(exception, code);
    }
}
