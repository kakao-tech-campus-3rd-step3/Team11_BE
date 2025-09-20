package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ProfileGetTest extends BaseProfileTest {

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
    }

    @Test
    @DisplayName("프로필 단건 조회 성공 - 200 OK")
    void getProfileById_success() {
        // given
        // ROLE_ADMIN으로 로그인하여 ROLE_USER의 프로필을 조회
        String accessToken = getToken(Role.ROLE_ADMIN).accessToken();

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}", test_user_profile_uuid)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(test_user_profile_uuid.toString()))
            .body("nickname", equalTo(TEST_USER_PROFILE_NICKNAME));
    }

    @Test
    @DisplayName("프로필 단건 조회 실패 - 404 Not Found (존재하지 않는 프로필 ID)")
    void getProfileById_fail_notFound() {
        // given
        String accessToken = getToken(Role.ROLE_ADMIN).accessToken();
        String nonExistentProfileId = "00000000-0000-0000-0000-000000000000";

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}", nonExistentProfileId)
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("프로필 단건 조회 실패 - 401 Unauthorized (토큰 없음)")
    void getProfileById_fail_unauthorized() {
        // when & then
        RestAssured
            .given().log().all()
            .when()
            .get("/{profileId}", test_user_profile_uuid)
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("평가하지 않은 모임 조회 성공 - 200 OK")
    void getUnEvaluatedMeetups_success() {
        // given
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .param("page", 0)
            .param("size", 5)
            .when()
            .get("/me/unEvaluated-meetups")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content", notNullValue())
            .body("content[0].meetupId", notNullValue())
            .body("content[0].unEvaluatedCount", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("평가하지 않은 모임 조회 실패 - 401 Unauthorized (토큰 없음)")
    void getUnEvaluatedMeetups_fail_unauthorized() {
        RestAssured
            .given().log().all()
            .when()
            .get("/me/unEvaluated-meetups")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}