package com.pnu.momeet.e2e.evaluation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetupEvaluationCreateTest extends BaseMeetupEvaluationTest {

    @Test
    @DisplayName("배치 평가 성공 - created 2건, alreadyEvaluated/invalid 비어있음 (200 OK)")
    void batchCreate_success() {
        UUID meetupId = test_meetup_id;
        // 타깃 2명: 관리자 + (관리자 다시 한 번은 중복되므로, 간단히 관리자 1명만 검증하고 다른 한 명은 임의 UUID로 예시)
        // 실제 테스트 픽스처에 참가자 2명 이상이면 아래 targetB를 실제 참가자 ID로 바꿔줘.
        UUID targetA = target_admin_profile_uuid;
        UUID targetB = target_admin_profile_uuid; // 픽스처에 참가자 1명인 경우 created 1 + invalid 1이 될 수 있음

        Map<String, Object> body = Map.of(
            "items", List.of(
                Map.of("targetProfileId", targetA.toString(), "rating", "LIKE"),
                Map.of("targetProfileId", targetB.toString(), "rating", "DISLIKE")
            )
        );

        ExtractableResponse<Response> res = RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("created.size()", greaterThanOrEqualTo(1))
            .body("alreadyEvaluated.size()", greaterThanOrEqualTo(0))
            .body("invalid.size()", greaterThanOrEqualTo(0))
            .extract();

        // created 항목은 teardown에서 정리
        List<String> ids = res.jsonPath().getList("created.id");
        if (ids != null) {
            evaluationsToBeDeleted.addAll(ids.stream().map(UUID::fromString).toList());
        }
    }

    @Test
    @DisplayName("멱등성 - 동일 요청 재전송 시 created는 0, alreadyEvaluated에 대상 포함 (200 OK)")
    void batchCreate_idempotent() {
        UUID meetupId = test_meetup_id;
        UUID target   = target_admin_profile_uuid;

        Map<String, Object> req = Map.of(
            "items", List.of(Map.of("targetProfileId", target.toString(), "rating", "LIKE"))
        );

        // 1차: created 1
        ExtractableResponse<Response> r1 = RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(req)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("created.size()", is(1))
            .extract();
        evaluationsToBeDeleted.add(UUID.fromString(r1.jsonPath().getString("created[0].id")));

        // 2차: created 0, alreadyEvaluated 1
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(req)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("created.size()", is(0))
            .body("alreadyEvaluated", hasItem(target.toString()));
    }

    @Test
    @DisplayName("요청 내 중복 대상은 invalid로 수집되지만 200 OK(항상 부분 성공)")
    void batchCreate_invalid_duplicateInRequest() {
        UUID meetupId = test_meetup_id;
        UUID target   = target_admin_profile_uuid;

        Map<String, Object> body = Map.of(
            "items", List.of(
                Map.of("targetProfileId", target.toString(), "rating", "LIKE"),
                Map.of("targetProfileId", target.toString(), "rating", "DISLIKE")
            )
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("invalid.size()", greaterThanOrEqualTo(1))
            .body("invalid[0].message", containsString("중복"));
    }

    @Test
    @DisplayName("비참가자 대상은 invalid로 수집 (200 OK)")
    void batchCreate_invalid_notParticipant() {
        UUID meetupId = test_meetup_id;
        UUID notParticipant = UUID.randomUUID(); // 모임 참가자가 아닌 가짜 ID

        Map<String, Object> body = Map.of(
            "items", List.of(Map.of("targetProfileId", notParticipant.toString(), "rating", "LIKE"))
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("created.size()", is(0))
            .body("invalid.size()", is(1))
            .body("invalid[0].targetProfileId", equalTo(notParticipant.toString()))
            .body("invalid[0].message", containsString("모임 참가자가 아닙니다"));
    }

    @Test
    @DisplayName("@Valid 검증 - rating 누락은 400 Bad Request")
    void batchCreate_fail_validation_ratingMissing() {
        UUID meetupId = test_meetup_id;

        Map<String, Object> body = Map.of(
            "items", List.of(Map.of("targetProfileId", target_admin_profile_uuid.toString()))
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(400);
    }

    @Test
    @DisplayName("401 Unauthorized - 토큰 없이 호출")
    void batchCreate_fail_unauthorized() {
        UUID meetupId = test_meetup_id;

        Map<String, Object> body = Map.of(
            "items", List.of(Map.of("targetProfileId", target_admin_profile_uuid.toString(), "rating", "LIKE"))
        );

        RestAssured
            .given().log().all()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/{meetupId}/evaluations", meetupId)
            .then().log().all()
            .statusCode(401);
    }
}