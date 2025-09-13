package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@Tag("read")
@DisplayName("E2E : Meetup 조회 테스트")
class MeetupReadTest extends BaseMeetupTest {

    private UUID createTestMeetup() {
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
                36.5,
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
    @DisplayName("모임 단건 조회 성공 테스트 - 200 OK")
    void get_meetup_by_id_success() {
        UUID meetupId = createTestMeetup();

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
                    "scoreLimit", equalTo(36.5f),
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
        createTestMeetup(); // 테스트용 모임 하나 생성

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
        createTestMeetup(); // GAME 카테고리 모임 생성

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
        createTestMeetup(); // 부산 지역 모임 생성

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
                .queryParam("latitude", 35.23203443995263)
                .queryParam("longitude", 129.08262659183725)
                .queryParam("radiusKm", 10.0)
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
        UUID meetupId = createTestMeetup();

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
}