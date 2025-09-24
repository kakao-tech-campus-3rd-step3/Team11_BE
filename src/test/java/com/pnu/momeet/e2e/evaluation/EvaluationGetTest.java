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
