package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.common.security.JwtTokenProvider;
import com.pnu.momeet.domain.auth.entity.RefreshToken;
import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
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
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    private static final long IAT_BUFFER_SECONDS = 10;

    private TokenPair generateTokenPair(UUID memberId) {
        // 토큰 발급 시점 기록 & 계정 활성화
        memberService.updateMemberById(memberId, member -> {
            member.setTokenIssuedAt(LocalDateTime.now().minusSeconds(IAT_BUFFER_SECONDS));
            member.setEnabled(true);
        });

        // 액세스 토큰과 리프레시 토큰 발급
        String accessToken = tokenProvider.generateAccessToken(memberId);
        String refreshToken = tokenProvider.generateRefreshToken(memberId);

        // 반환 이전에 refresh token 저장 또는 갱신
        refreshTokenRepository.save(new RefreshToken(memberId, refreshToken));

        return new TokenPair(refreshToken, accessToken);
    }

    @Transactional
    public TokenPair signUp(String email, String password) {
        Member savedMember = memberService.saveMember(new Member(email, password, List.of(Role.ROLE_USER)));
        return generateTokenPair(savedMember.getId());
    }

    @Transactional
    public TokenPair login(String email, String password) {

        Member member;
        try {
            member = memberService.findMemberByEmail(email);
        } catch (NoSuchElementException e) {
            throw new AuthenticationException("존재하지 않는 이메일이거나, 잘못된 비밀번호입니다.") {};
        }

        if (member.getProvider() != Provider.EMAIL) {
            throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {};
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthenticationException("존재하지 않는 이메일이거나, 잘못된 비밀번호입니다.") {};
        }

        if (!member.isAccountNonLocked()) {
            throw new AuthenticationException("잠긴 계정입니다. 관리자에게 문의하세요.") {};
        }

        return generateTokenPair(member.getId());
    }

    @Transactional
    public void logout(UUID memberId) {
        if (refreshTokenRepository.existsById(memberId)) {
            memberService.disableMemberById(memberId);
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
                () -> new AuthenticationException("로그아웃된 사용자입니다. 다시 로그인 해주세요.") {});

        if (!savedToken.getValue().equals(refreshToken)) {
            throw new AuthenticationException("유효하지 않은 리프레시 토큰입니다.") {};
        }

        return generateTokenPair(memberId);
    }
}
