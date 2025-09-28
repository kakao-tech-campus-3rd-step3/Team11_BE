package com.pnu.momeet.e2e.auth;

import com.pnu.momeet.domain.auth.dto.request.LoginRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.dto.response.MemberInfo;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;
@DisplayName("인증 E2E 테스트 - 로그인")
public class AuthLoginTest extends BaseAuthTest {

    @Test
    @DisplayName("로그인 성공 테스트")
    public void login_success() {
        LoginRequest request = new LoginRequest(testMember.email(), testPassword);
        TokenResponse response = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/login")
                .then()
                .log().all()
                .statusCode(200)
                .body(
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                )
                .extract()
                .as(TokenResponse.class);

        testTokenPair(response, testMember);

        MemberInfo loggedInMember = memberService.getMemberInfoById(testMember.id());
        assertThat(loggedInMember.enabled()).isTrue(); // 계정 활성화 여부
        assertThat(loggedInMember.tokenIssuedAt()).isNotNull(); // 토큰 발급 시점
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 요청(400 Bad Request)")
    public void login_fail_invalid_request() {
        List.of(
            new LoginRequest("invalidEmail", "testAuth123!"), // 잘못된 이메일
            new LoginRequest("test@test.com", "short") // 짧은 비밀번호
        ).forEach(request ->
            RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/login")
                .then()
                    .log().all()
                    .statusCode(400));
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 이메일 혹은 비밀번호(401 Unauthorized)")
    public void login_fail_invalid_email_or_password() {
        List.of(
                new LoginRequest("notExist@test.com", "testAuth123!"),
                new LoginRequest(testMember.email(), "testAuth1!")
        ).forEach(request ->
            RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(request)
                .when()
                    .post("/login")
                .then()
                    .log().all()
                    .statusCode(401)
        );
    }
}