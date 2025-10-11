package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.exception.UnMatchedPasswordException;
import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.auth.dto.request.LoginRequest;
import com.pnu.momeet.domain.auth.dto.request.RefreshRequest;
import com.pnu.momeet.domain.auth.dto.request.SignupRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.DefaultAuthService;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
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
    private final EmailAuthService emailAuthService;
    private final DefaultAuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> MemberSignUp(
            @Valid @RequestBody SignupRequest request
    ) {
        if (!request.password1().equals(request.password2())) {
            throw new UnMatchedPasswordException("비밀번호가 일치하지 않습니다.");
        }
        TokenResponse tokenResponse =
                emailAuthService.signUp(request.email(), request.password1());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> MemberLogin(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokenResponse = emailAuthService.login(request.email(), request.password());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> TokenRefresh(
            @Valid @RequestBody RefreshRequest request
            ) {
        TokenResponse tokenResponse = authService.refreshTokens(request.refreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> MemberLogout(
            @AuthenticationPrincipal UserDetails memberInfo
    ) {
        authService.logout(UUID.fromString(memberInfo.getUsername()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ws-upgrade")
    public ResponseEntity<String> websocketUpgrade(
            @AuthenticationPrincipal CustomUserDetails memberInfo
    ) {
        return ResponseEntity.ok(authService.createWsUpgradeToken(memberInfo.getMemberId()));
    }
}
