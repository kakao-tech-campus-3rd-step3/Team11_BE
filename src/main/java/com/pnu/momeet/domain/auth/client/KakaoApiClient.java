package com.pnu.momeet.domain.auth.client;

import com.pnu.momeet.domain.auth.dto.KakaoUserInfo;
import com.pnu.momeet.domain.auth.dto.request.KakaoTokenRequest;
import com.pnu.momeet.domain.auth.dto.response.KakaoTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApiClient {
    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.admin-key}")
    private String adminKey;

    @Value("${kakao.token-url}")
    private String tokenUrl;

    @Value("${kakao.user-info-url}")
    private String userInfoUrl;

    @Value("${kakao.unlink-url}")
    private String unlinkUrl;

    private final RestTemplate restTemplate;

    public String getKakaoAuthUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .build(true)
                .toUriString();
    }

    public KakaoTokenResponse getAccessToken(String code, String redirectUri) {
        KakaoTokenRequest request = new KakaoTokenRequest(
                "authorization_code",
                clientId,
                clientSecret,
                redirectUri,  // 프론트가 사용한 redirect_uri 사용
                code
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            String requestBody = convertToFormUrlEncoded(request);
            HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, httpEntity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new IllegalArgumentException("카카오에서 액세스 토큰을 받지 못했습니다.");
            }

            return new KakaoTokenResponse(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("token_type"),
                    (String) responseBody.get("refresh_token"),
                    (Integer) responseBody.get("expires_in"),
                    (String) responseBody.get("scope")
            );
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 인증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);

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
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public boolean unlinkUser(String kakaoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", kakaoId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            log.debug("카카오 연동 해제 API 요청: {}", unlinkUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(unlinkUrl, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("카카오 연동 해제 API 실패: HTTP {} - {}", response.getStatusCode(), kakaoId);
                return false;
            }

            Map<String, Object> responseBody = response.getBody();
            boolean success = responseBody != null && responseBody.containsKey("id");

            if (!success) {
                log.warn("카카오 연동 해제 API 응답 오류: {}", responseBody);
            }

            log.info("카카오 연동 해제 API 성공: {}", kakaoId);
            return success;
        } catch (Exception e) {
            log.error("카카오 연동 해제 실패: {}", e.getMessage());
            return false;
        }
    }

    private String convertToFormUrlEncoded(KakaoTokenRequest request) {
        return UriComponentsBuilder.newInstance()
                .queryParam("grant_type", request.grant_type())
                .queryParam("client_id", request.client_id())
                .queryParam("client_secret", request.client_secret())
                .queryParam("redirect_uri", request.redirect_uri())
                .queryParam("code", request.code())
                .build()
                .getQuery();
    }
}
