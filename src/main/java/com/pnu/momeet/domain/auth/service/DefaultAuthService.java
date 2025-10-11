package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.entity.RefreshToken;
import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final MemberEntityService memberService;

    private static final long IAT_BUFFER_SECONDS = 10;


    @Transactional
    public TokenResponse generateTokenPair(UUID memberId) {
        // 토큰 발급 시점 기록 & 계정 활성화
        Member member = memberService.getById(memberId);
        memberService.updateMember(member, m -> {
            m.setTokenIssuedAt(LocalDateTime.now().minusSeconds(IAT_BUFFER_SECONDS));
            m.setEnabled(true);
        });
        // 액세스 토큰과 리프레시 토큰 발급
        String accessToken = tokenProvider.generateAccessToken(memberId);
        String refreshToken = tokenProvider.generateRefreshToken(memberId);

        // 반환 이전에 refresh token 저장 또는 갱신
        refreshTokenRepository.save(new RefreshToken(memberId, refreshToken));

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(UUID memberId) {
        if (refreshTokenRepository.existsById(memberId)) {
            refreshTokenRepository.deleteById(memberId);
            log.info("로그아웃 성공: {}", memberId);
        }
        log.warn("로그아웃 시도: 존재하지 않는 회원 - {}", memberId);
    }

    @Transactional
    public TokenResponse refreshTokens(String refreshToken) {
        UUID memberId;
        try {
            Claims payload = tokenProvider.getPayload(refreshToken);
            memberId = UUID.fromString(payload.getSubject());
        } catch (ExpiredJwtException e) {
            log.info("리프레시 토큰 만료: {}", refreshToken);
            throw new AuthenticationException("리프레시 토큰이 만료되었습니다. 다시 로그인 해주세요.") {
            };
        } catch (Exception e) {
            log.info("리프레시 토큰 파싱 실패: {}", refreshToken);
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.") {
            };
        }

        var savedToken = refreshTokenRepository.findById(memberId);
        if (savedToken.isEmpty()) {
            log.info("리프레시 토큰 없음: {}", memberId);
            throw new AuthenticationException("로그아웃된 사용자입니다. 다시 로그인 해주세요.") {};
        }
        if (!savedToken.get().getValue().equals(refreshToken)) {
            // parsing이 되었지만 DB에 저장된 토큰과 다르다면, 탈취된 토큰일 가능성이 있으므로 warn 로그 남김
            log.warn("리프레시 토큰 불일치: {}", memberId);
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.") {};
        }
        var response =  generateTokenPair(memberId);
        log.info("토큰 재발급 성공: {}", memberId);
        return response;
    }

    public String createWsUpgradeToken(UUID memberId) {
        return tokenProvider.generateWsUpgradeToken(memberId);
    }
}
