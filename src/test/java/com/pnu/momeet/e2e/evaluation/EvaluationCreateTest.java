package com.pnu.momeet.e2e.evaluation;

import static org.hamcrest.Matchers.equalTo;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EvaluationCreateTest extends BaseEvaluationTest {

    @Test
    @DisplayName("평가 성공 - 200 OK")
    void createEvaluation_success() {
        UUID meetupId = test_meetup_id; // 실제로는 테스트용 meetup 생성 필요
        UUID targetProfileId = test_user_profile_uuid; // 예시로 테스트 계정 프로필 사용

        ExtractableResponse<Response> response = RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(Map.of(
                "meetupId", meetupId.toString(),
                "targetProfileId", targetProfileId.toString(),
                "rating", "LIKE"
            ))
            .when()
            .post()
            .then().log().all()
            .statusCode(200)
            .body("rating", equalTo("LIKE"))
            .extract();

        UUID createdEvalId = UUID.fromString(response.jsonPath().getString("id"));
        evaluationsToBeDeleted.add(createdEvalId);
    }

    @Test
    @DisplayName("실패 - 401 Unauthorized (토큰 없음)")
    void createEvaluation_fail_unauthorized() {
        RestAssured
            .given().log().all()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "meetupId", UUID.randomUUID().toString(),
                "targetProfileId", test_user_profile_uuid.toString(),
                "rating", "LIKE"
            ))
            .when()
            .post()
            .then().log().all()
            .statusCode(401);
    }

    @Test
    @DisplayName("실패 - 400 Bad Request (유효성 검사 실패)")
    void createEvaluation_fail_validation() {
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body(Map.of(
                "meetupId", UUID.randomUUID().toString(),
                "targetProfileId", test_user_profile_uuid.toString()
                // rating 누락
            ))
            .when()
            .post()
            .then().log().all()
            .statusCode(400);
    }
}
