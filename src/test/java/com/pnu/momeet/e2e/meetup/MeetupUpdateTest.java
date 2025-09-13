package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;

@Tag("update")
@DisplayName("E2E : Meetup 수정 테스트")
class MeetupUpdateTest extends BaseMeetupTest {

    private UUID createTestMeetup() {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구 부산대학로 63번길 2"
        );

        MeetupCreateRequest request = new MeetupCreateRequest(
                "수정 테스트 모임",
                "GAME",
                "BOARD_GAME",
                "수정할 예정인 모임입니다.",
                List.of("보드게임"),
                6,
                35.0,
                3,
                location
        );

        var response = RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .statusCode(201)
                .extract()
                .as(MeetupResponse.class);

        toBeDeleted.add(response.id());
        return response.id();
    }

    @Test
    @DisplayName("모임 수정 성공 테스트 - 200 OK")
    void update_meetup_success() {
        createTestMeetup(); // alice가 모임 생성

        LocationRequest newLocation = LocationRequest.of(
                35.1595454,
                129.0625775,
                "부산광역시 해운대구 해운대로 570"
        );

        MeetupUpdateRequest request = new MeetupUpdateRequest(
                "수정된 모임 이름",
                "SPORTS",
                "SOCCER",
                "수정된 모임 설명입니다.",
                List.of("축구", "운동", "친목"),
                8,
                37.0,
                newLocation
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "name", equalTo("수정된 모임 이름"),
                    "category", equalTo("SPORTS"),
                    "subCategory", equalTo("SOCCER"),
                    "description", equalTo("수정된 모임 설명입니다."),
                    "capacity", equalTo(8),
                    "scoreLimit", equalTo(37.0f),
                    "location.latitude", equalTo(newLocation.latitude().floatValue()),
                    "location.longitude", equalTo(newLocation.longitude().floatValue()),
                    "location.address", equalTo(newLocation.address()),
                    "updatedAt", notNullValue()
                );
    }

    @Test
    @DisplayName("모임 수정 성공 테스트 - 부분 수정")
    void update_meetup_success_partial() {
        createTestMeetup(); // alice가 모임 생성

        MeetupUpdateRequest request = new MeetupUpdateRequest(
                "부분 수정된 이름",
                null, // category 수정 안함
                null, // subCategory 수정 안함
                "부분 수정된 설명만 변경",
                null, // hashTags 수정 안함
                null, // capacity 수정 안함
                null, // scoreLimit 수정 안함
                null  // location 수정 안함
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "name", equalTo("부분 수정된 이름"),
                    "description", equalTo("부분 수정된 설명만 변경"),
                    // 기존 값들은 유지
                    "category", equalTo("GAME"),
                    "subCategory", equalTo("BOARD_GAME"),
                    "capacity", equalTo(6)
                );
    }

    @Test
    @DisplayName("유효성 검사 실패 테스트 - 400 Bad Request - 잘못된 값")
    void update_meetup_fail_by_validation_error() {
        createTestMeetup(); // alice가 모임 생성

        List<String> tooManyHashTags = IntStream.range(0, 21)
            .mapToObj(i -> "태그" + i)
            .toList();

        List.of(
                Map.of(
                    "name", "", // 빈 이름
                    "category", "GAME",
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "A".repeat(61), // 너무 긴 이름 (60자 초과)
                    "category", "GAME",
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 1, // 최소 인원 미만
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 101, // 최대 인원 초과 (100명 초과)
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", tooManyHashTags, // 너무 많은 해시태그
                    "capacity", 5,
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", -1.0 // 음수 점수 제한
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "INVALID_CATEGORY", // 잘못된 카테고리
                    "subCategory", "BOARD_GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "subCategory", "INVALID_SUB_CATEGORY", // 잘못된 서브카테고리
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                )
        ).forEach(body ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .put("/me")
                .then()
                    .log().all()
                    .statusCode(400)
                    .body("validationErrors", not(empty()))
        );
    }

    @Test
    @DisplayName("카테고리 불일치 실패 테스트 - 400 Bad Request")
    void update_meetup_fail_by_category_mismatch() {
        createTestMeetup(); // alice가 모임 생성

        List.of(
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME", // GAME 카테고리
                    "subCategory", "SOCCER", // SPORTS 서브카테고리 (불일치)
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "SPORTS", // SPORTS 카테고리
                    "subCategory", "BOARD_GAME", // GAME 서브카테고리 (불일치)
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "STUDY", // STUDY 카테고리
                    "subCategory", "BASKETBALL", // SPORTS 서브카테고리 (불일치)
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0
                )
        ).forEach(body ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .put("/me")
                .then()
                    .log().all()
                    .statusCode(400)
                    .body("validationErrors", not(empty()))
        );
    }

    @Test
    @DisplayName("모임 수정 실패 테스트 - 401 Unauthorized - 인증 실패")
    void update_meetup_fail_by_unauthorized() {
        createTestMeetup(); // alice가 모임 생성

        MeetupUpdateRequest request = new MeetupUpdateRequest(
                "수정할 모임",
                "GAME",
                "BOARD_GAME",
                "설명",
                List.of(),
                5,
                35.0,
                null
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/me")
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("모임 수정 실패 테스트 - 404 Not Found - 다른 사용자가 수정 시도")
    void update_meetup_fail_by_different_user() {
        createTestMeetup(); // alice가 모임 생성

        MeetupUpdateRequest request = new MeetupUpdateRequest(
                "다른 사용자가 수정 시도",
                "GAME",
                "BOARD_GAME",
                "설명",
                List.of(),
                5,
                35.0,
                null
        );

        // chris@test.com 사용자로 alice의 모임 수정 시도
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(BOB_EMAIL).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/me")
            .then()
                .log().all()
                .statusCode(404); // chris는 자신의 모임이 없으므로 404
    }

    @Test
    @DisplayName("모임 수정 실패 테스트 - 404 Not Found - 모임이 없는 사용자")
    void update_meetup_fail_by_no_meetup() {
        // 격리된 사용자 생성 (모임이 없는 사용자)
        String isolatedEmail = "no-meetup-" + java.util.UUID.randomUUID() + "@test.com";
        createTestUser(isolatedEmail);

        MeetupUpdateRequest request = new MeetupUpdateRequest(
                "수정할 모임",
                "GAME",
                "BOARD_GAME",
                "설명",
                List.of(),
                5,
                35.0,
                null
        );

        // 모임이 없는 사용자로 수정 시도
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(isolatedEmail).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/me")
            .then()
                .log().all()
                .statusCode(404);
    }
}