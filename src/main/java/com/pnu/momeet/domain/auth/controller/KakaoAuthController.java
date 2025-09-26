package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.common.security.util.TokenCookieManager;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.KakaoAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;
    private final TokenCookieManager tokenCookieManager;
    private final JwtTokenProvider jwtTokenProvider;

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

    // 카카오 회원 탈퇴
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> kakaoWithdraw(
            @RequestHeader("Authorization") String token,
            HttpServletResponse response
    ) {
        String cleanToken = token.replace("Bearer ", "");
        TokenInfo tokenInfo = jwtTokenProvider.parseToken(cleanToken);
        UUID memberId = UUID.fromString(tokenInfo.subject());

        kakaoAuthService.withdrawKakaoMember(memberId);

        tokenCookieManager.deleteAccessTokenCookie(response);
        tokenCookieManager.deleteRefreshTokenCookie(response);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "카카오 회원 탈퇴가 완료되었습니다.");

        return ResponseEntity.ok(responseBody);
    }
}
