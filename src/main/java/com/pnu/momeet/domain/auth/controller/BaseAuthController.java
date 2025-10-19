package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.auth.dto.request.RefreshRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.BaseAuthService;
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
public class BaseAuthController {
    private final BaseAuthService authService;

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
