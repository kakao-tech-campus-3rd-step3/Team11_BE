package com.pnu.momeet.common.security.interceptor;

import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.common.security.details.CustomUserDetailService;
import com.pnu.momeet.common.security.details.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandshakeInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailService userDetailService;
    @Value("${jwt.access-token.name-in-cookie}")
    private String accessTokenCookieName;


    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) throws Exception {
        try {
            HttpServletRequest servletRequest = null;
            if (request instanceof ServletServerHttpRequest servletServerRequest) {
                servletRequest = servletServerRequest.getServletRequest();
            }

            String token = extractToken(request, servletRequest);
            TokenInfo info = tokenProvider.parseToken(token);
            CustomUserDetails userDetails = userDetailService.loadUserByUsername(info.subject());
            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Stomp 세션에서 인증 정보 사용 가능
            attributes.put("SPRING.AUTHENTICATION", auth);
            return true;
        } catch (Exception e) {
            log.warn("웹소켓 핸드셰이크 인증 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception) {
    }

    private String extractToken(ServerHttpRequest request, HttpServletRequest servletRequest) {
        // 1) Cookie (via servlet request if available)
        if (servletRequest != null && servletRequest.getCookies() != null) {
            for (var cookie : servletRequest.getCookies()) {
                if (cookie.getName().equals(accessTokenCookieName)) {
                    return cookie.getValue();
                }
            }
        }
        // 2) Authorization header (Bearer)
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null && servletRequest != null) {
            authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        }
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(7);
        }

        return null;
    }

}
