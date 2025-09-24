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
import com.pnu.momeet.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

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
    private final MemberService memberService;
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

        return generateKakaoTokenPair(memberId);
    }

    private String getAccessTokenFromKakao(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=authorization_code" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&redirect_uri=" + redirectUri +
                "&code=" + code;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

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
            MemberResponse existingMember = memberService.findMemberByEmail(kakaoUserInfo.email());

            if (existingMember.provider() != Provider.KAKAO) {
                throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {};
            }

            if (!existingMember.isAccountNonLocked()) {
                throw new BannedAccountException("잠긴 계정입니다. 관리자에게 문의하세요.") {};
            }

            return existingMember.id();
        } catch (NoSuchElementException e) {
            MemberResponse newMember = memberService.saveMember(
                    new Member(
                            kakaoUserInfo.email(),
                            "",
                            Provider.KAKAO,
                            kakaoUserInfo.kakaoId(),
                            List.of(Role.ROLE_USER)
                    )
            );
            return newMember.id();
        }
    }

    private TokenResponse generateKakaoTokenPair(UUID memberId) {
        memberService.updateMemberById(memberId, member -> {
            member.setTokenIssuedAt(LocalDateTime.now().minusSeconds(IAT_BUFFER_SECONDS));
            member.setEnabled(true);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(memberId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        refreshTokenRepository.save(new RefreshToken(memberId, refreshToken));

        return new TokenResponse(accessToken, refreshToken);
    }
}
