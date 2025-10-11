package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.auth.dto.request.KakaoCallbackRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.dto.response.WithdrawResponse;
import com.pnu.momeet.domain.auth.service.KakaoAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;

    // 카카오 로그인 시작
    @GetMapping
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        String kakaoAuthUrl = kakaoAuthService.getKakaoAuthUrl();
        response.sendRedirect(kakaoAuthUrl);
    }

    // 카카오 로그인 콜백
    @GetMapping("/callback")
    public ResponseEntity<TokenResponse> kakaoCallback(
            @ModelAttribute KakaoCallbackRequest request
    ) {
        if (request.error() != null) {
            throw new IllegalArgumentException("카카오 로그인 실패: " + request.error_description());
        }

        TokenResponse tokenResponse = kakaoAuthService.kakaoLogin(request.code());
        return ResponseEntity.ok(tokenResponse);
    }

    // 카카오 회원 탈퇴
    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawResponse> kakaoWithdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID memberId = userDetails.getMemberId();

        kakaoAuthService.withdrawKakaoMember(memberId);
        return ResponseEntity.ok(new WithdrawResponse("카카오 회원 탈퇴가 완료되었습니다."));
    }
}
