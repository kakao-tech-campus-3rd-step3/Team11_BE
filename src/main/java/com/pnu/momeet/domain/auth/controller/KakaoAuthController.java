package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.auth.dto.request.KakaoCallbackRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.dto.response.WithdrawResponse;
import com.pnu.momeet.domain.auth.service.KakaoAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;

    // 카카오 로그인 콜백 - 프론트에서 code와 redirectUri를 받아서 처리
    @PostMapping("")
    public ResponseEntity<TokenResponse> kakaoCallback(
            @Valid @RequestBody KakaoCallbackRequest request
    ) {
        TokenResponse tokenResponse = kakaoAuthService.kakaoLogin(request.code(), request.redirectUri());
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
