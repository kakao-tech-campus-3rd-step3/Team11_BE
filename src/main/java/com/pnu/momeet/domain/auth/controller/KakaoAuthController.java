package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.security.util.TokenCookieManager;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.KakaoAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;
    private final TokenCookieManager tokenCookieManager;

    // 카카오 로그인 시작
    @GetMapping
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        String kakaoAuthUrl = kakaoAuthService.getKakaoAuthUrl();
        response.sendRedirect(kakaoAuthUrl);
    }

    // 카카오 로그인 콜백
    @GetMapping("/callback")
    public ResponseEntity<TokenResponse> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletResponse response
    ) {
        if (error != null) {
            throw new IllegalArgumentException("카카오 로그인 실패: " + error_description);
        }

        TokenResponse tokenResponse = kakaoAuthService.kakaoLogin(code);

        tokenCookieManager.saveAccessTokenToCookie(response, tokenResponse.accessToken());
        tokenCookieManager.saveRefreshTokenToCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(tokenResponse);
    }
}
