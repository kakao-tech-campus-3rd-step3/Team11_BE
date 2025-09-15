package com.pnu.momeet.common.security.filter;

import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.common.security.util.TokenCookieManager;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtTokenCookieHandingFilter  extends OncePerRequestFilter {

    private final TokenCookieManager tokenCookieManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailAuthService emailAuthService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> accessToken = tokenCookieManager.extractAccessTokenFromCookie(request);
        if (accessToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            tokenProvider.getClaims(accessToken.get());
            request.setAttribute(tokenCookieManager.getAccessTokenCookieName(), accessToken.get());
            filterChain.doFilter(request, response);
            return;

        } catch (ExpiredJwtException ignored) {
            try {
                Optional<String> refreshToken = tokenCookieManager.extractRefreshTokenFromCookie(request);
                if (refreshToken.isPresent()) {
                    TokenResponse tokenPair = emailAuthService.refreshTokens(refreshToken.get());
                    tokenCookieManager.saveAccessTokenToCookie(response, tokenPair.accessToken());
                    tokenCookieManager.saveRefreshTokenToCookie(response, tokenPair.refreshToken());
                    request.setAttribute(tokenCookieManager.getAccessTokenCookieName(), tokenPair.accessToken());
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception ignored1) { }
        } catch (Exception ignored) { }
        filterChain.doFilter(request, response);
    }
}
