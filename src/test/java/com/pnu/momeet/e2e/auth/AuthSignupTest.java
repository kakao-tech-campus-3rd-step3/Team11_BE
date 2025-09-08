package com.pnu.momeet.e2e.auth;

import com.pnu.momeet.domain.auth.dto.request.SignupRequest;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("인증 E2E 테스트 - 회원가입")
public class AuthSignupTest extends BaseAuthTest {

    @Test
    @DisplayName("회원가입 성공 테스트")
    public void signup_success() {
        SignupRequest request = new SignupRequest("testSignup@test.com", "testSignup123@", "testSignup123@");
        TokenResponse response = RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/signup")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                )
                .extract()
                .as(TokenResponse.class);

        Member signedUpMember = memberService.findMemberByEmail(request.email());
        toBeDeleted.add(signedUpMember); // 회원가입한 계정 삭제를 위해 리스트에 추가

        testTokenPair(response, signedUpMember);
        assertThat(signedUpMember).isNotNull();
        assertThat(signedUpMember.getProvider()).isEqualTo(Provider.EMAIL);
        assertThat(signedUpMember.isEnabled()).isTrue(); // 계정 활성화 여부
        assertThat(signedUpMember.getTokenIssuedAt()).isNotNull(); // 토큰 발급 시점
        assertThat(signedUpMember.getRoles().size()).isEqualTo(1);
        assertThat(signedUpMember.getRoles().getFirst().getName()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 잘못된 요청(400 Bad Request)")
    public void signup_fail_invalid_request() {
        List.of(
            Map.of("email", "invalidEmail", "password1", "testSignup123@", "password2", "testSignup123@"), // 잘못된 이메일
            Map.of("email", "valid@test.com", "password1", "short", "password2", "short"), // 짧은 비밀번호
            Map.of("email", "valid@test.com", "password1", "testSignup123@", "password2", "different123@") // 비밀번호 불일치
        ).forEach(body ->
            RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/signup")
            .then()
                .log().all()
                .statusCode(400)
                .body("validationErrors", notNullValue())
        );
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 중복된 이메일(409 Conflict)")
    public void signup_fail_duplicate_email() {
        SignupRequest request = new SignupRequest(testMember.getEmail(), "testSignup123@", "testSignup123@");
        RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/signup")
            .then()
                .log().all()
                .statusCode(409);
    }
}
