package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.exception.UnMatchedPasswordException;
import com.pnu.momeet.common.security.util.TokenCookieManager;
import com.pnu.momeet.domain.auth.dto.request.LoginRequest;
import com.pnu.momeet.domain.auth.dto.request.RefreshRequest;
import com.pnu.momeet.domain.auth.dto.request.SignupRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final EmailAuthService authService;
    private final TokenCookieManager tokenCookieManager;

    // TODO: swagger 적용하기

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> MemberSignUp(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response
    ) {
        if (!request.password1().equals(request.password2())) {
            throw new UnMatchedPasswordException("비밀번호가 일치하지 않습니다.");
        }
        TokenResponse tokenResponse =
                authService.signUp(request.email(), request.password1());

        tokenCookieManager.saveAccessTokenToCookie(response, tokenResponse.accessToken());
        tokenCookieManager.saveRefreshTokenToCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> MemberLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.login(request.email(), request.password());

        tokenCookieManager.saveAccessTokenToCookie(response, tokenResponse.accessToken());
        tokenCookieManager.saveRefreshTokenToCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> TokenRefresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletResponse response
            ) {
        TokenResponse tokenResponse = authService.refreshTokens(request.refreshToken());

        tokenCookieManager.saveAccessTokenToCookie(response, tokenResponse.accessToken());
        tokenCookieManager.saveRefreshTokenToCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> MemberLogout(
            @AuthenticationPrincipal UserDetails memberInfo,
            HttpServletResponse response
    ) {
        authService.logout(UUID.fromString(memberInfo.getUsername()));

        tokenCookieManager.deleteAccessTokenCookie(response);
        tokenCookieManager.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok().build();
    }
}
