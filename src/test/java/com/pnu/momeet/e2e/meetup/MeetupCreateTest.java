package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@Tag("create")
@DisplayName("E2E : Meetup 생성 테스트")
class MeetupCreateTest extends BaseMeetupTest {

    @Test
    @DisplayName("모임 생성 성공 테스트 - 201 Created")
    void create_meetup_success() {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구 부산대학로 63번길 2"
        );

        LocalDateTime base = baseSlot();
        String startAt = slot(base, 2); // +1h
        String endAt   = slot(base, 5); // +2.5h

        MeetupCreateRequest request = new MeetupCreateRequest(
                "테스트 모임",
                "GAME",
                "테스트용 보드게임 모임입니다.",
                List.of("보드게임", "친목"),
                6,
                35.0,
                startAt,
                endAt,
                location
        );

        var res = RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .contentType(ContentType.JSON)
                .log().body()
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(201)
                .body(
                    "id", notNullValue(),
                    "name", equalTo(request.name()),
                    "category", equalTo("GAME"),
                    "description", equalTo(request.description()),
                    "capacity", equalTo(request.capacity()),
                    "scoreLimit", equalTo(request.scoreLimit().floatValue()),
                    "status", equalTo("OPEN"),
                    "location.latitude", equalTo(location.latitude().floatValue()),
                    "location.longitude", equalTo(location.longitude().floatValue()),
                    "location.address", equalTo(location.address()),
                    "createdAt", notNullValue(),
                    "updatedAt", notNullValue(),
                    "endAt", notNullValue()
                )
                .extract()
                .as(MeetupDetail.class);

        // 삭제할 모임 목록에 추가
        toBeDeleted.add(res.id());
    }

    @Test
    @DisplayName("모임 생성 성공 테스트 - 기본값 적용")
    void create_meetup_success_with_defaults() {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구 부산대학로 63번길 2"
        );

        LocalDateTime base = baseSlot();
        String startAt = slot(base, 2); // +1h
        String endAt   = slot(base, 5); // +2.5h

        MeetupCreateRequest request = new MeetupCreateRequest(
                "기본값 테스트 모임",
                "SPORTS",
                null, // description default to ""
                null, // hashTags default to empty list
                null, // capacity default to 10
                null, // scoreLimit default to 0.0
                startAt,
                endAt,
                location
        );

        var res = RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(201)
                .body(
                    "id", notNullValue(),
                    "name", equalTo("기본값 테스트 모임"),
                    "category", equalTo("SPORTS"),
                    "description", equalTo(""),
                    "capacity", equalTo(10),
                    "scoreLimit", equalTo(0.0f),
                    "status", equalTo("OPEN")
                )
                .extract()
                .as(MeetupDetail.class);

        toBeDeleted.add(res.id());
    }

    @Test
    @DisplayName("유효성 검사 실패 테스트 - 400 Bad Request - 필수 필드 누락")
    void create_meetup_fail_by_validation_error_required_fields() {
        List.of(
                Map.of(
                    // name 누락
                    "category", "GAME",
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", Map.of(
                        "latitude", 35.23203443995263,
                        "longitude", 129.08262659183725,
                        "address", "부산광역시 금정구"
                    )
                ),
                Map.of(
                    "name", "테스트 모임",
                    // category 누락
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", Map.of(
                        "latitude", 35.23203443995263,
                        "longitude", 129.08262659183725,
                        "address", "부산광역시 금정구"
                    )
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME"
                    // startAt, endAt, location 누락
                )
        ).forEach(body ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .post()
                .then()
                    .log().all()
                    .statusCode(400)
                    .body("validationErrors", not(empty()))
        );
    }

    @Test
    @DisplayName("유효성 검사 실패 테스트 - 400 Bad Request - 잘못된 값")
    void create_meetup_fail_by_validation_error_invalid_values() {
        Map<String, Object> location = Map.of(
                "latitude", 35.23203443995263,
                "longitude", 129.08262659183725,
                "address", "부산광역시 금정구"
        );

        List.of(
                Map.of(
                    "name", "", // 빈 이름
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "A".repeat(61), // 너무 긴 이름 (60자 초과)
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 1, // 최소 인원 미만
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 100, // 최대 인원 초과 (99명 초과)
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T14:00", // 종료 시간이 시작 시간보다 이전
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0,
                    "startAt", "invalid-time", // 잘못된 시간 형식
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of("태그1", "태그2", "태그3", "태그4", "태그5", "태그6", "태그7", "태그8", "태그9", "태그10", "태그11"), // 해시태그 11개 (10개 초과)
                    "capacity", 5,
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "GAME",
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", -1.0, // 음수 점수 제한
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                ),
                Map.of(
                    "name", "테스트 모임",
                    "category", "INVALID_CATEGORY", // 잘못된 카테고리
                    "description", "설명",
                    "hashTags", List.of(),
                    "capacity", 5,
                    "scoreLimit", 35.0,
                    "startAt", "2025-10-18T15:00",
                    "endAt", "2025-10-18T18:00",
                    "location", location
                )
        ).forEach(body ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .post()
                .then()
                    .log().all()
                    .statusCode(400)
                    .body("validationErrors", not(empty()))
        );
    }


    @Test
    @DisplayName("모임 생성 실패 테스트 - 401 Unauthorized - 인증 실패")
    void create_meetup_fail_by_unauthorized() {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구"
        );

        MeetupCreateRequest request = new MeetupCreateRequest(
                "테스트 모임",
                "GAME",
                "설명",
                List.of(),
                5,
                35.0,
                "2025-10-18T15:00",
                "2025-10-18T18:00",
                location
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(401);
    }
}