package com.pnu.momeet.common.security.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import com.pnu.momeet.common.security.util.JwtAuthenticateHelper;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandShakeInterceptor implements HandshakeInterceptor {
    private final JwtAuthenticateHelper jwtAuthenticateHelper;
    
    @Override
    public boolean beforeHandshake(
        @NonNull ServerHttpRequest request,
        @NonNull ServerHttpResponse response,
        @NonNull WebSocketHandler wsHandler,
        @NonNull java.util.Map<String, Object> attributes
    ) throws Exception {
        try {
            String token = jwtAuthenticateHelper.resolveToken(request);
            var authenticationToken = jwtAuthenticateHelper.createAuthenticationToken(token);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            return true;
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            throw e;
        }
    }
    
    @Override
    public void afterHandshake(
        @NonNull ServerHttpRequest request,
        @NonNull ServerHttpResponse response,
        @NonNull WebSocketHandler wsHandler,
        @Nullable Exception exception
    ) {
        log.debug("WebSocket 핸드셰이크 완료");
    }

}
