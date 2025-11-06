package com.pnu.momeet.e2e.evaluation;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProfileEvaluationGetTest extends BaseProfileEvaluationTest {

    private String evaluatorToken; // user
    private String targetToken;    // admin
    private UUID evaluatorProfileId;
    private UUID targetProfileId;

    @BeforeEach
    void baseSetup() throws IOException {
        // 평가 생성은 /api/meetups의 상대경로를 쓰므로 basePath는 호출마다 지정
        evaluatorToken = getToken(Role.ROLE_USER).accessToken();
        targetToken    = getToken(Role.ROLE_ADMIN).accessToken();

        ensureMyProfile(evaluatorToken);
        ensureMyProfile(targetToken);

        evaluatorProfileId = fetchMyProfileId(evaluatorToken);
        targetProfileId    = fetchMyProfileId(targetToken);
    }

    @Test
    @DisplayName("최근 모임 조회 - 미평가만 (evaluated=false) 200 OK")
    void getMeetups_unevaluated_success() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        given().log().all()
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

        given().log().all()
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

        given().log().all()
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
        given().log().all()
            .param("page", 0)
            .param("size", 5)
            .when()
            .get("/me/meetups")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("삭제된 타깃에게는 평가 생성할 수 없다 (200 + invalid)")
    void cannotCreate_forDeletedTarget_minimal() {
        UUID meetupId = fetchEndedMeetupId(targetToken);
        UUID targetId = targetProfileId;

        // 타깃 삭제
        given().basePath("/api/profiles").header(AUTH_HEADER, BEAR_PREFIX + targetToken)
            .when().delete("/me")
            .then().statusCode(204);

        // 삭제된 타깃에게 평가 생성 → 200 + invalid 메시지
        given().basePath("/api/meetups").header(AUTH_HEADER, BEAR_PREFIX + evaluatorToken)
            .contentType(ContentType.JSON)
            .body("""
            {"items":[{"targetProfileId":"%s","rating":"LIKE"}]}
            """.formatted(targetId))
            .when().post("/{meetupId}/evaluations", meetupId)
            .then().statusCode(200)
            .body("invalid.size()", is(1))
            .body("invalid[0].message", containsString("모임 참가자가 아닙니다"));
    }

    @Test
    @DisplayName("타깃을 삭제하면 해당 평가는 CASCADE로 제거된다")
    void cascade_whenTargetDeleted_minimal() {
        UUID meetupId = fetchEndedMeetupId(targetToken);
        UUID targetId = targetProfileId;

        // 1) 평가 1건 생성(evaluator → target)
        ExtractableResponse<Response> res =
            given().basePath("/api/meetups").header(AUTH_HEADER, BEAR_PREFIX + evaluatorToken)
                .contentType(ContentType.JSON)
                .body("""
                {"items":[{"targetProfileId":"%s","rating":"LIKE"}]}
                """.formatted(targetId))
                .when().post("/{meetupId}/evaluations", meetupId)
                .then().statusCode(200)
                .extract();

        UUID createdId = UUID.fromString(res.jsonPath().getList("created.id", String.class).getFirst());
        assertThat(evaluationRepository.findById(createdId)).isPresent();

        // 2) 타깃 삭제 → 평가 CASCADE 제거
        given().basePath("/api/profiles").header(AUTH_HEADER, BEAR_PREFIX + targetToken)
            .when().delete("/me")
            .then().statusCode(204);

        assertThat(evaluationRepository.findById(createdId)).isNotPresent();
    }

    // helpers

    private UUID ensureMyProfile(String accessToken) {
        // 1) 먼저 조회
        var getRes = io.restassured.RestAssured.given().log().all()
            .basePath("/api/profiles")
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when().get("/me")
            .then().log().all()
            .extract();

        if (getRes.statusCode() == 200) {
            return java.util.UUID.fromString(getRes.jsonPath().getString("id"));
        }

        // 2) 없으면 생성 (ProfileCreateTest 스타일로 멀티파트 전송)
        String nickname = "test_" + java.util.UUID.randomUUID().toString().substring(0, 8);

        // 한글 안정 전송용 UTF-8 멀티파트
        var nicknamePart = new io.restassured.builder.MultiPartSpecBuilder(nickname)
            .controlName("nickname")
            .charset(java.nio.charset.StandardCharsets.UTF_8)
            .build();
        var descriptionPart = new io.restassured.builder.MultiPartSpecBuilder("테스트 자기소개입니다.")
            .controlName("description")
            .charset(java.nio.charset.StandardCharsets.UTF_8)
            .build();

        // 테스트 이미지 로드 (classpath: /image/badger.png)
        byte[] imageBytes = null;
        try {
            var resource = new org.springframework.core.io.ClassPathResource("/image/badger.png");
            imageBytes = resource.getInputStream().readAllBytes();
        } catch (Exception ignore) {
            // 이미지가 없어도 생성은 가능하도록 넘어가되, 로컬/CI에서 리소스 확인 권장
        }

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> createRes =
            io.restassured.RestAssured.given().log().all()
                .basePath("/api/profiles")
                .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
                .contentType(io.restassured.http.ContentType.MULTIPART)
                .multiPart(nicknamePart)
                .multiPart("age", 25)
                .multiPart("gender", "MALE")
                .multiPart(descriptionPart)
                .multiPart("baseLocation.baseLocationId", 26410) // 필드 경로 유지
                // 이미지가 있으면 첨부 (파일명/콘텐츠타입 ProfileCreateTest와 동일)
                .multiPart("image", "/image/badger.png", imageBytes, "image/png")
                .when().post("")
                .then().log().all()
                .statusCode(201) // 생성 성공
                .extract();

        // Location 헤더 및 본문 id 확인(선택 검증)
        // createRes.header("Location")가 "/api/profiles/" 포함하는지 검사 가능
        String id = createRes.jsonPath().getString("id");
        org.assertj.core.api.Assertions.assertThat(id).isNotBlank();
        return java.util.UUID.fromString(id);
    }

    // 종료된 모임에서 실제 존재하는 meetupId 1개 가져오기(존재 검증 포함, 간단 버전)
    private UUID fetchEndedMeetupId(String token) {
        ExtractableResponse<Response> res =
            given().basePath("/api/profiles").header(AUTH_HEADER, BEAR_PREFIX + token)
                .when().get("/me/meetups?page=0&size=5")
                .then().statusCode(200)
                .extract();

        List<String> ids = res.jsonPath().getList("content.meetupId");
        if (ids == null || ids.isEmpty()) {
            throw new IllegalStateException("테스트용 종료 모임이 필요합니다. (seed 또는 픽스처 확인)");
        }
        UUID id = UUID.fromString(ids.getFirst());

        // 존재 확인 한 번(간단)
        given().basePath("/api/meetups").header(AUTH_HEADER, BEAR_PREFIX + token)
            .when().get("/{meetupId}", id)
            .then().statusCode(200);

        return id;
    }

    private UUID fetchMyProfileId(String token) {
        String id =
            given().basePath("/api/profiles").header(AUTH_HEADER, BEAR_PREFIX + token)
                .when().get("/me")
                .then().statusCode(200)
                .extract().jsonPath().getString("id");
        return UUID.fromString(id);
    }
}
