package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@Tag("delete")
@DisplayName("E2E : Meetup 삭제 테스트")
class MeetupDeleteTest extends BaseMeetupTest {

    private UUID createTestMeetup() {
        LocationRequest location = LocationRequest.of(
                35.23203443995263,
                129.08262659183725,
                "부산광역시 금정구 부산대학로 63번길 2"
        );

        MeetupCreateRequest request = new MeetupCreateRequest(
                "삭제 테스트 모임",
                "GAME",
                "BOARD_GAME",
                "삭제할 예정인 모임입니다.",
                List.of("보드게임"),
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

        // 삭제 테스트에서는 자동 정리 목록에 추가하지 않음 (직접 삭제 테스트하므로)
        return response.id();
    }

    @Test
    @DisplayName("모임 삭제 성공 테스트 - 204 No Content - 소유자가 삭제")
    void delete_meetup_success_by_owner() {
        UUID meetupId = createTestMeetup(); // alice가 모임 생성

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .delete("/me")
            .then()
                .log().all()
                .statusCode(204);

        // 삭제된 모임 조회 시 404 확인
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .get("/{meetupId}", meetupId)
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("모임 삭제 성공 테스트 - 204 No Content - 관리자가 삭제")
    void delete_meetup_success_by_admin() {
        UUID meetupId = createTestMeetup(); // alice가 모임 생성

        // 관리자로 삭제
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .delete("/{meetupId}", meetupId)
            .then()
                .log().all()
                .statusCode(204);

        // 삭제된 모임 조회 시 404 확인
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .get("/{meetupId}", meetupId)
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("모임 삭제 실패 테스트 - 401 Unauthorized - 인증 실패")
    void delete_meetup_fail_by_unauthorized() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .delete("/me")
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("모임 삭제 실패 테스트 - 404 Not Found - 모임이 없는 사용자")
    void delete_meetup_fail_by_no_meetup() {
        // 격리된 사용자 생성 (모임이 없는 사용자)
        String isolatedEmail = "no-meetup-" + java.util.UUID.randomUUID() + "@test.com";
        createTestUser(isolatedEmail);
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(isolatedEmail).accessToken())
            .when()
                .delete("/me")
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("모임 삭제 실패 테스트 - 404 Not Found - 다른 사용자가 삭제 시도")
    void delete_meetup_fail_by_different_user() {
        UUID meetupId = createTestMeetup(); // alice가 모임 생성
        toBeDeleted.add(meetupId); // 테스트 후 정리를 위해 추가

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(BOB_EMAIL).accessToken())
            .when()
                .delete("/me")
            .then()
                .log().all()
                .statusCode(404); // chris는 모임이 없으므로 404
    }

    @Test
    @DisplayName("관리자 모임 삭제 실패 테스트 - 404 Not Found - 존재하지 않는 모임")
    void admin_delete_meetup_fail_by_not_found() {
        UUID nonExistentId = UUID.randomUUID();

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .delete("/{meetupId}", nonExistentId)
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("관리자 모임 삭제 실패 테스트 - 403 Forbidden - 권한 없음")
    void admin_delete_meetup_fail_by_forbidden() {
        UUID meetupId = createTestMeetup(); // alice가 모임 생성
        toBeDeleted.add(meetupId); // 테스트 후 정리를 위해 추가

        // 일반 사용자로 관리자 API 사용 시도
        String isolatedEmail = "forbidden-" + java.util.UUID.randomUUID() + "@test.com";
        createTestUser(isolatedEmail);
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(isolatedEmail).accessToken())
            .when()
                .delete("/{meetupId}", meetupId)
            .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("모임 중복 삭제 테스트 - 404 Not Found")
    void delete_meetup_twice_fail() {
        createTestMeetup(); // alice가 모임 생성

        // 첫 번째 삭제 - 성공
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .delete("/me")
            .then()
                .statusCode(204);

        // 두 번째 삭제 시도 - 실패
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .delete("/me")
            .then()
                .log().all()
                .statusCode(404);
    }
}