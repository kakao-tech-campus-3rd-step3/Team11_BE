package com.pnu.momeet.common.security.util;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import com.pnu.momeet.common.exception.BannedAccountException;
import com.pnu.momeet.common.exception.ConcurrentLoginException;
import com.pnu.momeet.common.exception.DisabledAccountException;
import com.pnu.momeet.common.exception.InvalidJwtTokenException;
import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.security.config.SecurityProperties;
import com.pnu.momeet.common.security.details.CustomUserDetailService;
import com.pnu.momeet.common.security.details.CustomUserDetails;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticateHelper {
    private final Pattern[] whitelistPatterns;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailService userDetailsService;
    private final String webSocketEndpoint;
    private final String tokenQueryParameter;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";


    public JwtAuthenticateHelper(
        SecurityProperties securityProperties,
        JwtTokenProvider jwtTokenProvider,
        CustomUserDetailService userDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        String[] whitelistUrls = securityProperties.getWhitelistUrls().toArray(new String[0]);
        this.whitelistPatterns = new Pattern[whitelistUrls.length];
        this.userDetailsService = userDetailsService;
        for (int i = 0; i < whitelistUrls.length; i++) {
            String regex = whitelistUrls[i]
                    .replace("**", ".*")  // '**'를 '.*'로 변경
                    .replace("*", "[^/]*"); // '*'를 '[^/]*'로 변경
            this.whitelistPatterns[i] = Pattern.compile(regex);
        }
        this.tokenQueryParameter = securityProperties.getWebsocket().getHandshakeHeader();
        this.webSocketEndpoint = securityProperties.getWebsocket().getEndpoint();
    }

    public boolean isWhitelisted(String path) {
        for (Pattern pattern : whitelistPatterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // WebSocket 연결 요청인 경우 쿼리 파라미터에서 토큰 추출
        if (request.getRequestURI().startsWith(webSocketEndpoint)) {
            String token = request.getParameter(tokenQueryParameter);
            if (token != null && !token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    public String resolveToken(StompHeaderAccessor accessor) {
        // message 헤더에서 nativeHeaders 추출
        String rawToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (rawToken instanceof String token && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public UserDetails verifyAndGetUserDetails(String token) throws AuthenticationException {
        try {
            TokenInfo tokenInfo = jwtTokenProvider.parseToken(Objects.requireNonNull(token));
            CustomUserDetails userDetails = userDetailsService.loadUserByUsername(tokenInfo.subject());
            LocalDateTime tokenIssuedAt = Objects.requireNonNull(userDetails.getTokenIssuedAt());

            if (!userDetails.isEnabled()) {
                log.info("비활성화된 계정 로그인 시도: {}", userDetails.getUsername());
                throw new DisabledAccountException("사용자 정보 변경 등으로 인해 일시적으로 비활성화된 계정입니다. 다시 로그인 해주세요.");
            }
            if (!userDetails.isAccountNonLocked()) {
                log.info("정지된 계정 로그인 시도: {}", userDetails.getUsername());
                throw new BannedAccountException("임시 제한 혹은 영구 정지된 계정입니다. 고객센터에 문의해주세요.");
            }

            if (tokenIssuedAt.isAfter(tokenInfo.issuedAt())) {
                log.info("동시 로그인 감지: {}, tokenIssuedAt: {}, tokenInfo.issuedAt(): {}",
                        userDetails.getUsername(), tokenIssuedAt, tokenInfo.issuedAt());
                throw new ConcurrentLoginException("다른 위치에서 로그인된 토큰입니다.");
            }
            return userDetails;
        } catch (ExpiredJwtException e) {
            throw new InvalidJwtTokenException("토큰이 만료되었습니다.");
        } catch (Exception e) {
            throw new InvalidJwtTokenException("유효하지 않은 토큰입니다.");
        }
    }

    public void verifyToken(String token) throws AuthenticationException {
        verifyAndGetUserDetails(token);
    }

    public UsernamePasswordAuthenticationToken createAuthenticationToken(String token) throws AuthenticationException {
        UserDetails userDetails = verifyAndGetUserDetails(token);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                token,
                userDetails.getAuthorities()
        );
    }
}
