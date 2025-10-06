package com.pnu.momeet.common.security.util;

import com.pnu.momeet.common.security.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class TokenCookieManager {

    private static final String COOKIE_PATH = "/";
    private static final Long IDLE_TIME_SECONDS = 5 * 60L; // 5 minutes

    @Getter
    private final String accessTokenCookieName;

    @Getter
    private final String refreshTokenCookieName;

    private final Long accessTokenExpirationSeconds;
    private final Long refreshTokenExpirationSeconds;
    private final boolean secureCookies;
    private final String sameSite;

    public TokenCookieManager(SecurityProperties securityProperties) {
        this.accessTokenCookieName = securityProperties.getJwt().getAccessToken().getNameInCookie();
        this.refreshTokenCookieName = securityProperties.getJwt().getRefreshToken().getNameInCookie();

        this.accessTokenExpirationSeconds = (long) securityProperties.getJwt().getAccessToken().getExpirationInSecond();
        this.refreshTokenExpirationSeconds = (long) securityProperties.getJwt().getRefreshToken().getExpirationInSecond();

        this.secureCookies = securityProperties.getHttps() != null && securityProperties.getHttps().isSecureCookies();
        this.sameSite = (secureCookies) ? "None" : "Lax";
    }


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

    public Optional<String> extractAccessTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, accessTokenCookieName);
    }
    public Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        return extractCookieValue(request, refreshTokenCookieName);
    }

    private ResponseCookie createCookie(String name, String value, Long maxAgeInSeconds) {
        var builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .path(COOKIE_PATH)
                .maxAge(maxAgeInSeconds);
            if (secureCookies) {
                builder = builder.sameSite(sameSite);
            }
            return builder.build();
    }

    public void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }


    public void saveAccessTokenToCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = createCookie(
                accessTokenCookieName,
                token,
                accessTokenExpirationSeconds - IDLE_TIME_SECONDS
        );
        addCookie(response, cookie);
    }

    public void saveRefreshTokenToCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = createCookie(
                refreshTokenCookieName,
                token,
                refreshTokenExpirationSeconds - IDLE_TIME_SECONDS
        );
        addCookie(response, cookie);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createCookie(accessTokenCookieName, null, 0L);
        addCookie(response, cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createCookie(refreshTokenCookieName, null, 0L);
        addCookie(response, cookie);
    }
}
