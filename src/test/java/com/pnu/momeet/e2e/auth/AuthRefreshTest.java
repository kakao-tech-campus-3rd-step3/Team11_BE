package com.pnu.momeet.e2e.auth;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.domain.auth.dto.RefreshRequest;
import com.pnu.momeet.domain.auth.dto.TokenResponse;
import com.pnu.momeet.domain.auth.entity.RefreshToken;
import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DisplayName("인증 E2E 테스트 - 토큰 재발급")
public class AuthRefreshTest extends BaseAuthTest {

    @Autowired
    private EmailAuthService emailAuthService;
    private Member loggedInMember;
    private TokenPair loginnedInTokenPair;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        loggedInMember = new Member("testRefrsh@test.com", testPassword, List.of(Role.ROLE_USER));
        loggedInMember = memberService.saveMember(loggedInMember);
        loginnedInTokenPair = emailAuthService.login(loggedInMember.getEmail(), testPassword);

        toBeDeleted.add(loggedInMember);
    }

    @Test
    @DisplayName("토큰 재발급 성공 테스트")
    public void refresh_success() {
        RefreshRequest request = new RefreshRequest(loginnedInTokenPair.refreshToken());
        TokenResponse pair = RestAssured
            .given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/refresh")
            .then()
                .log().all()
                .statusCode(200)
                .extract()
                .as(TokenResponse.class);

        testTokenPair(pair, loggedInMember);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 리프레시 토큰 (400 Bad Request)")
    public void refresh_fail_invalid_request() {
        List.of(
            new RefreshRequest("short"), // 너무 짧은 토큰
            new RefreshRequest("this.is.not.a.valid.token") // 형식에 맞지 않는 토큰
        ).forEach(request ->
            RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/refresh")
                .then()
                    .log().all()
                    .statusCode(400)
        );
    }
    
    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 리프레시 토큰 (401 Unauthorized)")
    public void refresh_fail_expired_token() {
        // 리프레시 토큰을 강제로 만료시킴
        RefreshToken refreshToken = new RefreshToken(
            loggedInMember.getId(),
            jwtTokenProvider.generateToken(loggedInMember.getId().toString(), -1000L) // 이미 만료된 토큰 생성
        );
        refreshTokenRepository.save(refreshToken); // DB에 저장

        RefreshRequest request = new RefreshRequest(refreshToken.getValue());
        RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/refresh")
            .then()
                .log().all()
                .statusCode(401);
    }
}

