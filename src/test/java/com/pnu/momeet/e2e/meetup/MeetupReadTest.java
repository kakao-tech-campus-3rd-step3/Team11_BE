package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@Tag("read")
@DisplayName("E2E : Meetup 조회 테스트")
class MeetupReadTest extends BaseMeetupTest {

    private UUID createTestMeetupByEmail(String email) {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구 부산대학로 63번길 2"
        );

        MeetupCreateRequest request = new MeetupCreateRequest(
                "조회 테스트 모임",
                "GAME",
                "BOARD_GAME",
                "테스트용 보드게임 모임입니다.",
                List.of("보드게임", "친목"),
                6,
                35.,
                3,
                location
        );

        var response = meetupService.createMeetup(
                request,
                users.get(email).id()
        );
        toBeDeleted.add(response.id());
        return response.id();
    }

    @Test
    @DisplayName("모임 단건 조회 성공 테스트 - 200 OK")
    void get_meetup_by_id_success() {
        UUID meetupId = createTestMeetupByEmail(ALICE_EMAIL);

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .get("/{meetupId}", meetupId)
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(meetupId.toString()),
                    "name", equalTo("조회 테스트 모임"),
                    "category", equalTo("GAME"),
                    "subCategory", equalTo("BOARD_GAME"),
                    "description", equalTo("테스트용 보드게임 모임입니다."),
                    "capacity", equalTo(6),
                    "scoreLimit", equalTo(35.0f),
                    "status", equalTo("OPEN"),
                    "location", notNullValue(),
                    "sigungu", notNullValue(),
                    "createdAt", notNullValue(),
                    "updatedAt", notNullValue(),
                    "endAt", notNullValue()
                );
    }

    @Test
    @DisplayName("모임 페이지 조회 성공 테스트 - 200 OK")
    void get_meetup_page_success() {
        createTestMeetupByEmail(ALICE_EMAIL); // 테스트용 모임 하나 생성

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sigunguCode", 26410) // 부산 금정구
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "content", notNullValue(),
                    "page", notNullValue(),
                    "page.totalElements", greaterThanOrEqualTo(1),
                    "page.totalPages", greaterThanOrEqualTo(1),
                    "page.size", equalTo(10),
                    "page.number", equalTo(0)
                );
    }

    @Test
    @DisplayName("모임 페이지 조회 성공 테스트 - 카테고리 필터링")
    void get_meetup_page_with_category_filter_success() {
        createTestMeetupByEmail(ALICE_EMAIL); // GAME 카테고리 모임 생성

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sigunguCode", 26410) // 부산 금정구
                .queryParam("category", "GAME")
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "content", notNullValue(),
                    "content.findAll { it.category == 'GAME' }.size()", 
                    greaterThanOrEqualTo(1)
                );
    }

    @Test
    @DisplayName("모임 지역 기반 조회 성공 테스트 - 200 OK")
    void get_meetup_by_geo_success() {
        createTestMeetupByEmail(ALICE_EMAIL); // 부산 지역 모임 생성

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .queryParams(Map.of(
                    "latitude", 35.23203443995263,
                    "longitude", 129.08262659183725,
                    "radius", 10.0
                ))
            .when()
                .get("/geo")
            .then()
                .log().all()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].location", notNullValue());
    }

    @Test
    @DisplayName("모임 조회 실패 테스트 - 404 Not Found - 존재하지 않는 ID")
    void get_meetup_by_id_fail_not_found() {
        UUID nonExistentId = UUID.randomUUID();
        
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .get("/{meetupId}", nonExistentId)
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("모임 조회 실패 테스트 - 401 Unauthorized - 인증 실패")
    void get_meetup_fail_by_unauthorized() {
        UUID meetupId = createTestMeetupByEmail(ALICE_EMAIL);

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .get("/{meetupId}", meetupId)
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("모임 지역 기반 조회 실패 테스트 - 400 Bad Request - 잘못된 파라미터")
    void get_meetup_by_geo_fail_by_invalid_params() {
        List.of(
                // latitude 누락
                "?longitude=129.08262659183725&radius=10.0",
                // longitude 누락
                "?latitude=35.23203443995263&radius=10.0",
                // 음수 반경
                "?latitude=35.23203443995263&longitude=129.08262659183725&radius=-1.0",
                // 0 반경 (1보다 작음)
                "?latitude=35.23203443995263&longitude=129.08262659183725&radius=0.5",
                // 너무 큰 반경 (100보다 큼)
                "?latitude=35.23203443995263&longitude=129.08262659183725&radius=101.0"
        ).forEach(queryString ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .when()
                    .get("/geo" + queryString)
                .then()
                    .log().all()
                    .statusCode(400)
        );
    }

    @Test
    @DisplayName("[차단] viewer가 모임 소유자를 차단하면 상세는 404")
    void get_meetup_by_id_blocked_by_viewer_404() {
        // given: BOB이 모임을 만들고, ALICE가 BOB을 차단
        UUID meetupId = createTestMeetupByEmail(BOB_EMAIL);
        block(users.get(ALICE_EMAIL).id(), users.get(BOB_EMAIL).id()); // ALICE → BOB

        // when & then: ALICE로 상세 조회 시 404
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
            .get("/{meetupId}", meetupId)
            .then()
            .log().all()
            .statusCode(404);
    }

    @Test
    @DisplayName("[차단] 모임 소유자가 viewer를 차단해도 상세는 404")
    void get_meetup_by_id_blocked_by_owner_404() {
        UUID meetupId = createTestMeetupByEmail(BOB_EMAIL);
        block(users.get(BOB_EMAIL).id(), users.get(ALICE_EMAIL).id()); // BOB → ALICE

        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
            .get("/{meetupId}", meetupId)
            .then()
            .log().all()
            .statusCode(404);
    }

    @Test
    @DisplayName("[차단] 페이지 조회 시 차단 모임은 목록에서 제외된다")
    void get_meetup_page_excludes_blocked_meetup() {
        UUID meetupId = createTestMeetupByEmail(BOB_EMAIL);
        block(users.get(ALICE_EMAIL).id(), users.get(BOB_EMAIL).id()); // ALICE → BOB

        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sigunguCode", 26410)
            .when()
            .get()
            .then()
            .log().all()
            .statusCode(200)
            // content에 해당 ID가 없어야 함
            .body("content.findAll { it.id == '" + meetupId + "' }.size()", equalTo(0));
    }

    @Test
    @DisplayName("[차단] 지도 조회: viewer가 모임 소유자와 차단이면 제외된다")
    void get_meetup_geo_excludes_when_viewer_blocks_owner() {
        // given: 모임은 BOB이 만듦(= owner=BOB)
        UUID meetupId = createTestMeetupByEmail(BOB_EMAIL);

        // ALICE -> BOB 차단
        block(users.get(ALICE_EMAIL).id(), users.get(BOB_EMAIL).id());

        // when & then: ALICE로 조회 시 해당 모임이 결과에 없어야 함
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .queryParams(Map.of(
                "latitude", 35.23203443995263,
                "longitude", 129.08262659183725,
                "radius", 10.0
            ))
            .when()
            .get("/geo")
            .then()
            .statusCode(200)
            .body("findAll { it.id == '" + meetupId + "' }.size()", equalTo(0));
    }

    @Test
    @DisplayName("[차단] 지도 조회: viewer가 참가자 중 누군가와 차단이면 제외된다")
    void get_meetup_geo_excludes_when_viewer_blocks_participant() {
        // given: owner=ALICE
        UUID meetupId = createTestMeetupByEmail(ALICE_EMAIL);

        // BOB을 참가자로 넣기 (기존 참가자 API 사용)
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(BOB_EMAIL).accessToken())
            .pathParam("meetupId", meetupId)
            .when()
            .post("/{meetupId}/participants")
            .then()
            .statusCode(200);

        // ALICE -> BOB 차단
        block(users.get(ALICE_EMAIL).id(), users.get(BOB_EMAIL).id());

        // when & then: ALICE로 조회 시 해당 모임이 결과에 없어야 함
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .queryParams(Map.of(
                "latitude", 35.23203443995263,
                "longitude", 129.08262659183725,
                "radius", 10.0
            ))
            .when()
            .get("/geo")
            .then()
            .statusCode(200)
            .body("findAll { it.id == '" + meetupId + "' }.size()", equalTo(0));
    }
}