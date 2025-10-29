package com.pnu.momeet.common.security.handler;

import com.pnu.momeet.common.security.util.JwtAuthenticateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {
    private final JwtAuthenticateHelper jwtAuthenticateHelper;

    @Override
    protected Principal determineUser(
            @NonNull ServerHttpRequest request,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        String token = jwtAuthenticateHelper.resolveTokenFromQueryParam(request);
        if (token == null || token.isEmpty()) {
            return null;
        }
        Authentication authentication = jwtAuthenticateHelper.createAuthenticationToken(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
}
