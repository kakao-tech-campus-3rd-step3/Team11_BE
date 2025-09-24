package com.pnu.momeet.e2e.evaluation;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class EvaluationGetTest extends BaseEvaluationTest {

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
            .param("evaluated", false)
            .when()
            .get("/me/meetups")
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
            .param("evaluated", false)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("evaluated=true는 아직 미지원 - 400 Bad Request")
    void getMeetups_fail_evaluated_true_not_supported() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .param("evaluated", true)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
