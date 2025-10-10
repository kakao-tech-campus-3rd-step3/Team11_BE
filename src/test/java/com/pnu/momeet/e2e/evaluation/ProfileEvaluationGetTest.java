package com.pnu.momeet.e2e.evaluation;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ProfileEvaluationGetTest extends BaseProfileEvaluationTest {

    @Test
    @DisplayName("최근 모임 조회 - 미평가만 (evaluated=false) 200 OK")
    void getMeetups_unevaluated_success() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .param("page", 0)
            .param("size", 5)
            .param("evaluated", false)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content", notNullValue())
            .body("content[0].meetupId", notNullValue())
            .body("content[0].evaluated", equalTo(false));
    }

    @Test
    @DisplayName("최근 모임 조회 - 평가함만 (evaluated=true) 200 OK")
    void getMeetups_evaluated_success() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .param("page", 0)
            .param("size", 5)
            .param("evaluated", true)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content[0].meetupId", nullValue())
            .body("content[0].evaluated", nullValue());
    }

    @Test
    @DisplayName("최근 모임 조회 - 혼합 목록(파라미터 없음) 200 OK")
    void getMeetups_mixed_success() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .param("page", 0)
            .param("size", 5)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content", notNullValue())
            .body("content[0].meetupId", notNullValue())
            .body("content[0].evaluated", anyOf(equalTo(true), equalTo(false)));
    }

    @Test
    @DisplayName("최근 모임 조회 - 토큰 없음 401")
    void getMeetups_fail_unauthorized() {
        RestAssured
            .given().log().all()
            .param("page", 0)
            .param("size", 5)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
