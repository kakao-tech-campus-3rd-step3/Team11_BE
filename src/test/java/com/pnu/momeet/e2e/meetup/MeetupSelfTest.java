package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
// removed unused import
import io.restassured.RestAssured;
// removed unused import
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

@Tag("self")
@DisplayName("E2E : Meetup 본인 모임 조회 테스트")
class MeetupSelfTest extends BaseMeetupTest {

    private UUID createTestMeetup() {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구 부산대학로 63번길 2"
        );

        MeetupCreateRequest request = new MeetupCreateRequest(
                "본인 모임 조회 테스트",
                "GAME",
                "BOARD_GAME",
                "본인 모임 조회 테스트용 모임입니다.",
                List.of("보드게임"),
                6,
                36.5,
                3,
                location
        );

       var response = meetupService.createMeetup(
                request,
                users.get(ALICE_EMAIL).id() // alice가 모임 생성
        );

        toBeDeleted.add(response.id());
        return response.id();
    }

    @Test
    @DisplayName("본인 모임 조회 성공 테스트 - 200 OK")
    void get_my_meetup_success() {
        UUID meetupId = createTestMeetup(); // alice가 모임 생성

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(meetupId.toString()),
                    "name", equalTo("본인 모임 조회 테스트"),
                    "category", equalTo("GAME"),
                    "subCategory", equalTo("BOARD_GAME"),
                    "description", equalTo("본인 모임 조회 테스트용 모임입니다."),
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
    @DisplayName("본인 모임 조회 실패 테스트 - 404 Not Found - 모임이 없는 사용자")
    void get_my_meetup_fail_by_no_meetup() {
        // 격리된 사용자 생성 (모임이 없는 사용자)
        String isolatedEmail = "no-meetup-" + java.util.UUID.randomUUID() + "@test.com";
        createTestUser(isolatedEmail);
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(isolatedEmail).accessToken())
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("본인 모임 조회 실패 테스트 - 401 Unauthorized - 인증 실패")
    void get_my_meetup_fail_by_unauthorized() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("본인 모임 조회 성공 테스트 - 모임 생성 후 즉시 조회")
    void get_my_meetup_success_after_creation() {
        LocationRequest location = LocationRequest.of(
                35.1595454,
                129.0625775,
                "부산광역시 해운대구 해운대로 570"
        );

        MeetupCreateRequest createRequest = new MeetupCreateRequest(
                "새로 생성한 모임",
                "SPORTS",
                "SOCCER",
                "새로 생성한 모임입니다.",
                List.of("축구", "운동"),
                10,
                35.0,
                2,
                location
        );

        // chris가 모임 생성
        var createdMeetup = meetupService.createMeetup(
                createRequest,
                users.get(BOB_EMAIL).id()
        );
        toBeDeleted.add(createdMeetup.id());

        // 바로 본인 모임 조회
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(BOB_EMAIL).accessToken())
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(createdMeetup.id().toString()),
                    "name", equalTo("새로 생성한 모임"),
                    "category", equalTo("SPORTS"),
                    "subCategory", equalTo("SOCCER"),
                    "status", equalTo("OPEN")
                );
    }

    @Test
    @DisplayName("다른 사용자 모임 조회 테스트 - 본인 모임만 조회됨")
    void get_my_meetup_only_own() {
        // alice가 모임 생성
        UUID aliceMeetupId = createTestMeetup();

        // chris가 모임 생성
        LocationRequest location = LocationRequest.of(
                35.1595454,
                129.0625775,
                "부산광역시 해운대구 해운대로 570"
        );

        MeetupCreateRequest chrisRequest = new MeetupCreateRequest(
                "chris의 모임",
                "SPORTS",
                "BASKETBALL",
                "chris가 만든 모임입니다.",
                List.of("농구"),
                8,
                36.0,
                2,
                location
        );

        var chrisMeetup = meetupService.createMeetup(
                chrisRequest,
                users.get(BOB_EMAIL).id() // chris가 모임 생성
        );

        toBeDeleted.add(chrisMeetup.id());

        // alice가 자신의 모임 조회 - alice의 모임만 나와야 함
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(aliceMeetupId.toString()),
                    "name", equalTo("본인 모임 조회 테스트") // alice의 모임
                );

        // chris가 자신의 모임 조회 - chris의 모임만 나와야 함
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(BOB_EMAIL).accessToken())
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(chrisMeetup.id().toString()),
                    "name", equalTo("chris의 모임") // chris의 모임
                );
    }
}