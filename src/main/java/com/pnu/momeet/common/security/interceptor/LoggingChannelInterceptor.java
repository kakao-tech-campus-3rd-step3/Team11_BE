package com.pnu.momeet.common.security.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ChannelInterceptor;

@Slf4j
public class LoggingChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message,@NonNull MessageChannel channel) {
        MessageHeaders headers = message.getHeaders();
        log.debug("세션 ID: {}, 명령: {}, 목적지: {}",
            headers.get("simpSessionId"),
            headers.get("simpMessageType"),
            headers.get("simpDestination")
        );
        return message;
    }

}
