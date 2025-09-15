package com.pnu.momeet.common.security.interceptor;

import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.common.security.details.CustomUserDetailService;
import com.pnu.momeet.common.security.details.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandshakeInterceptor {
    static final String ACCESS_TOKEN_KEY = "access_token";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailService userDetailService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        try {
            TokenInfo info = tokenProvider.parseToken(extractToken(servletRequest));
            CustomUserDetails userDetails = userDetailService.loadUserByUsername(info.subject());
            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Stomp 세션에서 인증 정보 사용 가능
            attributes.put("SPRING.AUTHENTICATION", auth);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
    Exception exception) {

    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getParameter(ACCESS_TOKEN_KEY);
        if (token != null && !token.isEmpty()) {
            return token;
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (ACCESS_TOKEN_KEY.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
