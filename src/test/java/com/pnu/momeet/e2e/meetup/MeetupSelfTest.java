package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
// removed unused import
import io.restassured.RestAssured;
// removed unused import
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        LocalDateTime base = baseSlot();
        String startAt = slot(base, 2); // +1h
        String endAt   = slot(base, 5); // +2.5h

        MeetupCreateRequest request = new MeetupCreateRequest(
            "본인 모임 조회 테스트",
            "GAME",
            "본인 모임 조회 테스트용 모임입니다.",
            List.of("보드게임"),
            6,
            35.0,
            startAt,
            endAt,
            location
        );

        var response = meetupService.createMeetup(request, users.get(ALICE_EMAIL).id());
        toBeDeleted.add(response.id());
        return response.id();
    }

    @Test
    @DisplayName("내 현재 모임 단건 조회 - 방장으로 조회 성공 (200)")
    void get_my_meetup_success_as_owner() {
        UUID meetupId = createTestMeetup(); // alice가 방장

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
                "description", equalTo("본인 모임 조회 테스트용 모임입니다."),
                "capacity", equalTo(6),
                "scoreLimit", equalTo(35.0f),
                "status", anyOf(equalTo("OPEN"), equalTo("IN_PROGRESS")),
                "location", notNullValue(),
                "sigungu", notNullValue(),
                "createdAt", notNullValue(),
                "updatedAt", notNullValue(),
                "endAt", notNullValue()
            );
    }

    @Test
    @DisplayName("내 현재 모임 단건 조회 - 참가자로 조회 성공 (200)")
    void get_my_meetup_success_as_participant() {
        // 1) alice가 모임 생성(방장)
        UUID meetupId = createTestMeetup();

        // 2) bob이 alice의 모임에 '활성 참가자'로 참여
        //    - 실제 참여 API 명칭은 프로젝트에 맞게 사용하세요 (joinMeetup/participate 등)
        participantService.joinMeetup(meetupId, users.get(CHRIS_EMAIL).id());

        // 3) bob이 /me 로 조회하면 방장이 아니더라도 '현재 참여 중' 모임이 단건 반환
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(CHRIS_EMAIL).accessToken())
            .when()
            .get("/me")
            .then()
            .log().all()
            .statusCode(200)
            .body(
                "id", equalTo(meetupId.toString()),
                "name", equalTo("본인 모임 조회 테스트"),
                "status", anyOf(equalTo("OPEN"), equalTo("IN_PROGRESS"))
            );
    }

    @Test
    @DisplayName("내 현재 모임 단건 조회 - 모임이 없는 사용자면 404")
    void get_my_meetup_fail_by_no_meetup() {
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
    @DisplayName("내 현재 모임 단건 조회 - 인증 실패 시 401")
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
    @DisplayName("모임 생성 직후 단건 조회 - 방장 시나리오 (200)")
    void get_my_meetup_success_after_creation() {
        LocationRequest location = LocationRequest.of(
            35.1595454,
            129.0625775,
            "부산광역시 해운대구 해운대로 570"
        );

        // ✅ LocalDateTime 기준 미래 시간
        LocalDateTime base = LocalDateTime.now();
        String startAt = slot(base, 2); // +1h
        String endAt   = slot(base, 5); // +2.5h

        MeetupCreateRequest createRequest = new MeetupCreateRequest(
            "새로 생성한 모임",
            "SPORTS",
            "새로 생성한 모임입니다.",
            List.of("축구", "운동"),
            10,
            35.0,
            startAt,
            endAt,
            location
        );

        var createdMeetup = meetupService.createMeetup(
            createRequest,
            users.get(CHRIS_EMAIL).id() // bob이 방장
        );
        toBeDeleted.add(createdMeetup.id());

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(CHRIS_EMAIL).accessToken())
            .when()
            .get("/me")
            .then()
            .log().all()
            .statusCode(200)
            .body(
                "id", equalTo(createdMeetup.id().toString()),
                "name", equalTo("새로 생성한 모임"),
                "category", equalTo("SPORTS"),
                // 상태 전이에 따라 OPEN 또는 IN_PROGRESS 허용
                "status", anyOf(equalTo("OPEN"), equalTo("IN_PROGRESS"))
            );
    }

    @Test
    @DisplayName("내 현재 모임 단건 조회 - 각자 자신의 모임만 조회됨")
    void get_my_meetup_only_own() {
        // alice가 모임 생성(방장)
        UUID aliceMeetupId = createTestMeetup();

        // bob이 다른 모임(방장)
        LocationRequest location = LocationRequest.of(
            35.1595454,
            129.0625775,
            "부산광역시 해운대구 해운대로 570"
        );

        LocalDateTime base = baseSlot();
        String startAt2 = slot(base, 2); // +1h
        String endAt2   = slot(base, 5); // +2.5h

        MeetupCreateRequest bobRequest = new MeetupCreateRequest(
            "bob의 모임",
            "SPORTS",
            "bob이 만든 모임입니다.",
            List.of("농구"),
            8,
            36.0,
            startAt2,
            endAt2,
            location
        );
        var bobMeetup = meetupService.createMeetup(bobRequest, users.get(CHRIS_EMAIL).id());
        toBeDeleted.add(bobMeetup.id());

        // alice → 자신의 모임이 나와야 함
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
                "name", equalTo("본인 모임 조회 테스트")
            );

        // bob → 자신의 모임이 나와야 함
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(CHRIS_EMAIL).accessToken())
            .when()
            .get("/me")
            .then()
            .log().all()
            .statusCode(200)
            .body(
                "id", equalTo(bobMeetup.id().toString()),
                "name", equalTo("bob의 모임")
            );
    }
}