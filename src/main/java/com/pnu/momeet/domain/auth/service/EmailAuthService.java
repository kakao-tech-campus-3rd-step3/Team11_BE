package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.common.security.JwtTokenProvider;
import com.pnu.momeet.domain.auth.entity.RefreshToken;
import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    private TokenPair generateTokenPair(UUID memberId) {
        String accessToken = tokenProvider.generateAccessToken(memberId);
        String refreshToken = tokenProvider.generateRefreshToken(memberId);
        return new TokenPair(refreshToken, accessToken);
    }

    private void saveOrUpdateRefreshToken(UUID memberId, String refreshToken) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findById(memberId);
        RefreshToken token;
        if (existingToken.isPresent()) {
            token = existingToken.get();
            token.setValue(refreshToken);
        } else {
            token = new RefreshToken(memberId, refreshToken);
        }
        refreshTokenRepository.save(token);
    }

    private void updateLastLoginAt(UUID memberId) {
        memberService.updateMemberById(memberId, m ->
                m.setLastLoginAt(LocalDateTime.now().minusSeconds(10)));
    }

    @Transactional
    public TokenPair signUp(String email, String password) {
        Member member = memberService.saveMember(email, password, List.of("ROLE_USER") );
        TokenPair tokenPair = generateTokenPair(member.getId());

        updateLastLoginAt(member.getId());
        saveOrUpdateRefreshToken(member.getId(), tokenPair.refreshToken());
        return tokenPair;
    }

    @Transactional
    public TokenPair login(String email, String password) {
        Member member = memberService.findMemberByEmail(email);
        if (member.getProvider() != Provider.EMAIL) {
            throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {
            };
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다.") {
            };
        }
        if (!member.isEnabled()) {
            throw new AuthenticationException("탈퇴한 회원입니다. 다시 가입해 주세요.") {
            };
        }

        updateLastLoginAt(member.getId());
        TokenPair tokenPair = generateTokenPair(member.getId());
        saveOrUpdateRefreshToken(member.getId(), tokenPair.refreshToken());
        return tokenPair;
    }

    @Transactional
    public void logout(UUID memberId) {
        if (refreshTokenRepository.existsById(memberId)) {
            refreshTokenRepository.deleteById(memberId);
        }
    }

    @Transactional
    public TokenPair refreshTokens(String refreshToken) {
        UUID memberId;
        try {
            Claims payload = tokenProvider.getPayload(refreshToken);
            memberId = UUID.fromString(payload.getSubject());
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("리프레시 토큰이 만료되었습니다. 다시 로그인 해주세요.") {
            };
        } catch (Exception e) {
            throw new AuthenticationException("유효하지 않은 리프레시 토큰입니다.") {
            };
        }

        RefreshToken savedToken = refreshTokenRepository.findById(memberId).orElseThrow(
                () -> new AuthenticationException("로그아웃된 사용자입니다. 다시 로그인 해주세요.") {
        });

        if (!savedToken.getValue().equals(refreshToken)) {
            throw new AuthenticationException("유효하지 않은 리프레시 토큰입니다.") {
            };
        }

        updateLastLoginAt(memberId);
        TokenPair pair = generateTokenPair(memberId);
        saveOrUpdateRefreshToken(memberId, pair.refreshToken());

        return pair;
    }
}
