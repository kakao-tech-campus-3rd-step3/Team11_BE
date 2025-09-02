package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProfileSelfTest extends BaseProfileTest {

    @Test
    @DisplayName("내 프로필 조회 성공 - 200 OK")
    void getMyProfile_success() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body("id", notNullValue())
                .body("nickname", equalTo(TEST_USER_PROFILE_NICKNAME))
                .body("age", equalTo(TEST_USER_PROFILE_AGE))
                .body("gender", anyOf(equalTo("MALE"), equalTo("FEMALE")))
                .body("baseLocation", equalTo(TEST_USER_PROFILE_LOCATION))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @DisplayName("내 프로필 조회 실패 - 401 Unauthorized (토큰 없음)")
    void getMyProfile_fail_unauthorized() {
        RestAssured
            .given()
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("내 프로필 조회 실패 - 401 Unauthorized (잘못된 토큰)")
    void getMyProfile_fail_unauthorized_invalidToken() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid.token.value")
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("내 프로필 조회 실패 - 404 Not Found (프로필 없음)")
    void getMyProfile_fail_notFound_profileMissing() {
        // admin 계정에는 프로필이 없음
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .get("/me")
            .then()
                .statusCode(404)
                .body("detail", containsString("프로필이 존재하지 않습니다."));
    }
}