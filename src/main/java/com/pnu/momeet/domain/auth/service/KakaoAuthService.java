package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.common.exception.BannedAccountException;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.domain.auth.client.KakaoApiClient;
import com.pnu.momeet.domain.auth.dto.KakaoUserInfo;
import com.pnu.momeet.domain.auth.dto.response.KakaoTokenResponse;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.entity.RefreshToken;
import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private static final long IAT_BUFFER_SECONDS = 10;

    private final MemberEntityService memberEntityService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoApiClient kakaoApiClient;

    public String getKakaoAuthUrl() {
        return kakaoApiClient.getKakaoAuthUrl();
    }

    public KakaoUserInfo getKakaoUserInfo(String code) {
        KakaoTokenResponse tokenResponse = kakaoApiClient.getAccessToken(code);
        KakaoUserInfo userInfoResponse = kakaoApiClient.getUserInfo(tokenResponse.access_token());

        return new KakaoUserInfo(userInfoResponse.kakaoId(), userInfoResponse.email());
    }

    public TokenResponse kakaoLogin(String code) {
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(code);

        UUID memberId = findOrCreateKakaoMember(kakaoUserInfo);

        TokenResponse tokenResponse = generateKakaoTokenPair(memberId);

        log.info("카카오 로그인 성공: {}", kakaoUserInfo.email());
        return tokenResponse;
    }

    @Transactional
    private UUID findOrCreateKakaoMember(KakaoUserInfo kakaoUserInfo) {
        try {
            Member existingMember = memberEntityService.getByEmail(kakaoUserInfo.email());

            if (existingMember.getProvider() != Provider.KAKAO) {
                log.warn("카카오 로그인 실패: 지원하지 않는 경로 - {} - {}", existingMember.getProvider(), kakaoUserInfo.email());
                throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {};
            }

            if (!existingMember.isAccountNonLocked()) {
                log.info("카카오 로그인 실패: 잠긴 계정 - {}", kakaoUserInfo.email());
                throw new BannedAccountException("잠긴 계정입니다. 관리자에게 문의하세요.") {};
            }

            return existingMember.getId();
        } catch (NoSuchElementException e) {
            return signupKakaoMember(kakaoUserInfo);
        }
    }

    private UUID signupKakaoMember(KakaoUserInfo kakaoUserInfo) {
        Member newMember = memberEntityService.saveMember(
                new Member(kakaoUserInfo.email(), "", Provider.KAKAO, kakaoUserInfo.kakaoId(), List.of(Role.ROLE_USER))
        );
        log.info("카카오 회원가입 성공: {}", kakaoUserInfo.email());
        return newMember.getId();
    }

    private TokenResponse generateKakaoTokenPair(UUID memberId) {
        Member member = memberEntityService.getById(memberId);

        memberEntityService.updateMember(member, m -> {
            m.setTokenIssuedAt(LocalDateTime.now().minusSeconds(IAT_BUFFER_SECONDS));
            m.setEnabled(true);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(memberId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        refreshTokenRepository.save(new RefreshToken(memberId, refreshToken));

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public void withdrawKakaoMember(UUID memberId) {
        Member member = memberEntityService.getById(memberId);

        if (member.getProvider() != Provider.KAKAO) {
            log.info("카카오 회원 탈퇴 실패: 카카오 회원이 아님 - {} ({})", member.getEmail(), member.getProvider());
            throw new IllegalArgumentException("카카오 회원이 아닙니다.");
        }

        log.info("카카오 연동 해제 API 호출 시작: {}", member.getProviderId());
        boolean unlinkSuccess = kakaoApiClient.unlinkUser(member.getProviderId());

        if (!unlinkSuccess) {
            log.error("카카오 연동 해제 실패: {} ({})", member.getEmail(), member.getProviderId());
            throw new IllegalArgumentException("카카오 연동 해제 실패로 탈퇴할 수 없습니다.");
        }

        memberEntityService.deleteById(memberId);
        log.info("카카오 연동 해제 및 회원 탈퇴 완료: {}", member.getEmail());
    }
}
