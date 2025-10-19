package com.pnu.momeet.common.security.filter;

import com.pnu.momeet.common.model.enums.TokenType;
import com.pnu.momeet.common.security.util.JwtAuthenticateHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthenticateHelper jwtAuthenticateHelper;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    @Value("${app.security.websocket.endpoint}")
    private String WebSocketEndpoint;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 화이트리스트 경로는 토큰 검사 없이 통과
        if (jwtAuthenticateHelper.isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isWebSocketRequest = requestPath.startsWith(WebSocketEndpoint);
        try {
            String token = isWebSocketRequest ?
                    jwtAuthenticateHelper.resolveTokenFromQueryParam(request) : // 웹소켓으로 부터 Upgrade 토큰 추출
                    jwtAuthenticateHelper.resolveToken(request);

            // 필요한 토큰 타입 결정
            TokenType requriedTokenType = isWebSocketRequest ? TokenType.WS_UPGRADE : TokenType.ACCESS;

            var authenticationToken = jwtAuthenticateHelper.createAuthenticationToken(token, requriedTokenType);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);

        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, e);
        }
    }
}
