package com.pnu.momeet.e2e.auth;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DisplayName("인증 E2E 테스트 - 로그아웃")
public class AuthLogoutTest extends BaseAuthTest {
    @Autowired
    private EmailAuthService emailAuthService;
    private TokenResponse tokenRes;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        MemberCreateRequest request = new MemberCreateRequest("testLogout@test.com", testPassword, List.of("ROLE_USER"));
        MemberResponse loggedInMember = memberService.saveMember(request);
        tokenRes = emailAuthService.login(loggedInMember.email(), testPassword);

        toBeDeleted.add(loggedInMember);
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    public void logout_success() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + tokenRes.accessToken())
            .when()
                .post("/logout")
            .then()
                .log().all()
                .statusCode(200);
    }

    @Test
    @DisplayName("로그아웃 실패 테스트 - 인증 실패(401 Unauthorized)")
    public void logout_fail_unauthorized() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalidToken")
            .when()
                .post("/logout")
            .then()
                .log().all()
                .statusCode(401);
    }
}
