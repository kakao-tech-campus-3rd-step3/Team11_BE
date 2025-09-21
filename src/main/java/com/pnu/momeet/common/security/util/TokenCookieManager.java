package com.pnu.momeet.common.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class TokenCookieManager {

    private static final String COOKIE_PATH = "/";
    private static final Long IDLE_TIME_SECONDS = 5 * 60L; // 5 minutes

    @Getter
    @Value("${jwt.access-token.name_in_cookie}")
    private String accessTokenCookieName;

    @Getter
    @Value("${jwt.refresh-token.name_in_cookie}")
    private String refreshTokenCookieName;

    @Value("${jwt.access-token.expiration_in_second}")
    private Long accessTokenExpirationSeconds;
    @Value("${jwt.refresh-token.expiration_in_second}")
    private Long refreshTokenExpirationSeconds;


    private Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
             Optional<String> value = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue);
             if (value.isPresent() && !value.get().isEmpty()) {
                 return value;
             }
        }
        return Optional.empty();
    }

    private Cookie setCookie(String name, String value, Long maxAgeInSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        // TODO : secure 옵션 설정 (https 환경에서만 쿠키 전송)
        // cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(Math.toIntExact(maxAgeInSeconds));
        return cookie;
    }

    public Optional<String> extractAccessTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, accessTokenCookieName);
    }
    public Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, refreshTokenCookieName);
    }

    public void saveAccessTokenToCookie(HttpServletResponse response, String token) {
        Cookie cookie = setCookie(
                accessTokenCookieName,
                token,
                accessTokenExpirationSeconds - IDLE_TIME_SECONDS
        );
        response.addCookie(cookie);
    }

    public void saveRefreshTokenToCookie(HttpServletResponse response, String token) {
        Cookie cookie = setCookie(
                refreshTokenCookieName,
                token,
                refreshTokenExpirationSeconds - IDLE_TIME_SECONDS
        );
        response.addCookie(cookie);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = setCookie(accessTokenCookieName, null, 0L);
        response.addCookie(cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = setCookie(refreshTokenCookieName, null, 0L);
        response.addCookie(cookie);
    }
}
