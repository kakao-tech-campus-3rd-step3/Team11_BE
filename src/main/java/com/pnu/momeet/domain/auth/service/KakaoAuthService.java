package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.common.exception.BannedAccountException;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.domain.auth.dto.KakaoUserInfo;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.entity.RefreshToken;
import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberDomainService;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private static final long IAT_BUFFER_SECONDS = 10;

    private final RestTemplate restTemplate;
    private final MemberEntityService memberEntityService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public String getKakaoAuthUrl() {
        return "https://kauth.kakao.com/oauth/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri;
    }

    public KakaoUserInfo getKakaoUserInfo(String code) {
        String token = getAccessTokenFromKakao(code);
        return getKakaoUserInfoFromToken(token);
    }

    public TokenResponse kakaoLogin(String code) {
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(code);

        UUID memberId = findOrCreateKakaoMember(kakaoUserInfo);

        TokenResponse tokenResponse = generateKakaoTokenPair(memberId);

        log.info("카카오 로그인 성공: {}", kakaoUserInfo.email());
        return tokenResponse;
    }

    private String getAccessTokenFromKakao(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new IllegalArgumentException("카카오에서 액세스 토큰을 받지 못했습니다.");
            }

            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            throw new IllegalArgumentException("카카오 인증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private KakaoUserInfo getKakaoUserInfoFromToken(String token) {
        String url = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null || !userInfo.containsKey("id") || !userInfo.containsKey("kakao_account")) {
                throw new IllegalArgumentException("카카오에서 사용자 정보를 받지 못했습니다.");
            }

            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            if (kakaoAccount == null || !kakaoAccount.containsKey("email")) {
                throw new IllegalArgumentException("카카오 계정에서 이메일 정보를 받지 못했습니다.");
            }

            return new KakaoUserInfo(
                    String.valueOf(userInfo.get("id")),
                    (String) kakaoAccount.get("email")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("카카오 사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private UUID findOrCreateKakaoMember(KakaoUserInfo kakaoUserInfo) {
        try {
            Member existingMember = memberEntityService.getByEmail(kakaoUserInfo.email());

            if (existingMember.getProvider() != Provider.KAKAO) {
                log.warn("카카오 로그인 실패: 지원하지 않는 경로 - {} - {}", existingMember.getProvider(), kakaoUserInfo.email());
                throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {};
            }

            if (!existingMember.isAccountNonLocked()) {
                log.warn("카카오 로그인 실패: 잠긴 계정 - {}", kakaoUserInfo.email());
                throw new BannedAccountException("잠긴 계정입니다. 관리자에게 문의하세요.") {};
            }

            return existingMember.getId();
        } catch (NoSuchElementException e) {
            Member newMember = memberEntityService.saveMember(
                    new Member(
                            kakaoUserInfo.email(),
                            "",
                            Provider.KAKAO,
                            kakaoUserInfo.kakaoId(),
                            List.of(Role.ROLE_USER)
                    )
            );
            log.info("카카오 회원가입 성공: {}", kakaoUserInfo.email());
            return newMember.getId();
        }
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
}
