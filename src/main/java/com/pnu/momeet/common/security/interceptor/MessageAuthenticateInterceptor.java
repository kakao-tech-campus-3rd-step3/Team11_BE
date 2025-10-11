package com.pnu.momeet.common.security.interceptor;

import com.pnu.momeet.common.security.util.JwtAuthenticateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAuthenticateInterceptor implements ChannelInterceptor {
    private final JwtAuthenticateHelper jwtAuthenticateHelper;
    private final static String STANDARD_ERROR_PATH = "/user/queue/errors";

    @Override
    public Message<?> preSend(
            @NonNull Message<?> message,
            @NonNull MessageChannel channel
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith(STANDARD_ERROR_PATH)) {
            // 에러 메시지는 인증 절차를 거치지 않고 그대로 통과시킴
            return message;
        }
        switch (Objects.requireNonNull(accessor.getMessageType())) {
            // CONNECT, SUBSCRIBE 요청은 토큰 검증 및 로깅 수행
            case CONNECT -> {
                String token = jwtAuthenticateHelper.resolveToken(accessor);
                Authentication authentication = jwtAuthenticateHelper.createAuthenticationToken(token);
                // connect 시 SecurityContext에 인증 정보 저장
                accessor.setUser(authentication);
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                log.info("웹소켓 연결 성공: userId={}, sessionId={}",
                        userDetails.getUsername(), accessor.getSessionId());
            }
            case SUBSCRIBE -> {
                String token = jwtAuthenticateHelper.resolveToken(accessor);
                UserDetails userDetails = jwtAuthenticateHelper.verifyAndGetUserDetails(token);
                log.info("웹소켓 구독 성공: userId={}, sessionId={}, destination={}",
                        userDetails.getUsername(), accessor.getSessionId(), destination);
            }

            // 메시지 요청은 로깅은 하지 않고 토큰 검증만 수행
            case MESSAGE -> {
                String token = jwtAuthenticateHelper.resolveToken(accessor);
                jwtAuthenticateHelper.verifyToken(token);
            }
            // 그 외의 요청은 특별한 처리를 하지 않음(예: DISCONNECT)
        }

        // 모든 검증을 통과한 메시지는 그대로 반환
        return message;
    }
}
