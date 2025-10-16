package com.pnu.momeet.domain.auth.controller;

import com.pnu.momeet.common.exception.UnMatchedPasswordException;
import com.pnu.momeet.domain.auth.dto.request.LoginRequest;
import com.pnu.momeet.domain.auth.dto.request.SignupRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailAuthController {
    private final EmailAuthService emailAuthService;
    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/signup")
    public ResponseEntity<Void> MemberSignUp(
            @Valid @RequestBody SignupRequest request
    ) {
        if (!request.password1().equals(request.password2())) {
            throw new UnMatchedPasswordException("비밀번호가 일치하지 않습니다.");
        }
        emailAuthService.signUp(request.email(), request.password1());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> MemberLogin(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokenResponse = emailAuthService.login(request.email(), request.password());
        return ResponseEntity.ok(tokenResponse);
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam("code") UUID code
    ) {
        emailAuthService.verifyEmail(code);
        return ResponseEntity.status(302).header("Location", frontendUrl).build();
    }
}
