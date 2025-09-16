package com.pnu.momeet.common.security.handler;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(
        @NonNull ServerHttpRequest request,
        @NonNull WebSocketHandler wsHandler,
        @NonNull Map<String, Object> attributes
) {
        var auth =  (Authentication) attributes.get("SPRING.AUTHENTICATION");
        return (Principal) auth.getPrincipal();
    }
}