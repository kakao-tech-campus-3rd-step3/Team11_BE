package com.pnu.momeet.e2e.evaluation;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static software.amazon.awssdk.http.HttpStatusCode.NOT_FOUND;
import static software.amazon.awssdk.http.HttpStatusCode.OK;
import static software.amazon.awssdk.http.HttpStatusCode.UNAUTHORIZED;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class MeetupEvaluationGetTest extends BaseMeetupEvaluationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private UUID findTestMeetupId() {
        // 테스트 SQL 픽스처로 생성된 '종료된 테스트 모임'을 사용
        UUID id = jdbcTemplate.queryForObject(
            "SELECT id FROM public_test.meetup WHERE name = ? ORDER BY created_at DESC LIMIT 1",
            (rs, rowNum) -> (UUID) rs.getObject("id"),
            "종료된 테스트 모임"
        );
        assertNotNull(id, "테스트용 모임이 존재해야 합니다. (SQL 픽스처 확인)");
        return id;
    }

    @Test
    @DisplayName("평가 후보 조회 성공 - 200 OK & 자기 자신 제외")
    void getCandidates_success_excludesSelf() {
        // given: 픽스처로 만든 종료된 테스트 모임 ID 조회(참가자: 관리자 + 테스트유저)
        UUID meetupId = findTestMeetupId();
        var accessToken = getToken(Role.ROLE_USER).accessToken();

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{meetupId}/evaluations/candidates", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("$", notNullValue())
            .body("size()", greaterThanOrEqualTo(0))
            // DTO 필드 예시 검증 (실제 응답 필드에 맞게 필요 시 조정)
            .body("profileId", everyItem(notNullValue()))
            .body("nickname", everyItem(notNullValue()))
            // 자기 자신은 후보에서 제외되었는지 확인
            .body("profileId", not(hasItem(evaluator_profile_uuid.toString())));
    }

    @Test
    @DisplayName("인증 없이 호출하면 401 Unauthorized")
    void getCandidates_unauthorized() {
        RestAssured
            .given().log().all()
            .when()
            .get("/{meetupId}/evaluations/candidates", UUID.randomUUID())
            .then().log().all()
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 모임이면 404 Not Found")
    void getCandidates_meetupNotFound() {
        var accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{meetupId}/evaluations/candidates", UUID.randomUUID())
            .then().log().all()
            .statusCode(404);
    }

    @Test
    @DisplayName("평가 후보 조회 - evaluator→target 24h 쿨타임 위반이면 후보에 나타나지 않는다")
    void getCandidates_excludesTargetWithin24hCooldown() {
        // given - 종료된 테스트 모임, 토큰 준비
        UUID meetupId = findTestMeetupId();
        var accessToken = getToken(Role.ROLE_USER).accessToken();

        // 선행 평가(지금 시각) 저장 -> 24h 쿨타임 위반
        Evaluation recent = Evaluation.create(
            meetupId,
            evaluator_profile_uuid,       // evaluator = user@test.com
            target_admin_profile_uuid,    // target = admin@test.com
            Rating.LIKE,
            "ip#cooldown"
        );
        evaluationRepository.save(recent);

        // when & then - 후보 호출 시 타겟이 목록에 없어야 함
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{meetupId}/evaluations/candidates", meetupId)
            .then().log().all()
            .statusCode(200)
            .body("$", not(hasItem(target_admin_profile_uuid.toString())))
            .body("profileId", not(hasItem(target_admin_profile_uuid.toString())));
    }
}
