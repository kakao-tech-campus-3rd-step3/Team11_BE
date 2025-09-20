package com.pnu.momeet.e2e.participant;

import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("participant")
@DisplayName("E2E : 밋업 나가기 테스트")
public class ParticipantLeaveTest extends BaseParticipantTest {


    @Test
    @DisplayName("일반 참가자 밋업 나가기 성공 - 204 No Content")
    void leaveMeetup_regularParticipant_success() {
        // Given: 밋업 생성 및 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 사용자가 밋업에 참가
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200);
        
        // When & Then: 밋업 나가기
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(204); // No Content
        
        // 참가자 목록에서 제거되었는지 확인
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(1)) // 호스트만 남음
                .body("[0].profile.nickname", equalTo(profiles.get(0).getNickname()));
    }

    @Test
    @DisplayName("여러 참가자 중 일부 나가기 성공 - 204 No Content")
    void leaveMeetup_partialLeave_success() {
        // Given: 밋업 생성 및 여러 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 여러 사용자가 밋업에 참가
        for (int i = 1; i < 4; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens.get(i).accessToken())
                    .pathParam("meetupId", meetup.id())
            .when()
                    .post("/{meetupId}/participants")
            .then()
                    .statusCode(200);
        }
        
        // When: 한 명이 밋업을 나감
        given()
                .header("Authorization", "Bearer " + tokens.get(2).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(204);
        
        // Then: 참가자 목록 확인 (호스트 + 2명 남음)
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(3))
                .body("profile.nickname", hasItems(
                        profiles.get(0).getNickname(),
                        profiles.get(1).getNickname(),
                        profiles.get(3).getNickname()
                ))
                .body("profile.nickname", not(hasItem(profiles.get(2).getNickname())));
    }

    @Test
    @DisplayName("참가하지 않은 밋업 나가기 실패 - 404 Not Found")
    void leaveMeetup_notParticipant_fail() {
        // Given: 밋업 생성 (사용자는 참가하지 않음)
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 참가하지 않은 사용자가 나가기 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(404); // Not Found 또는 409 Conflict
    }

    @Test
    @DisplayName("존재하지 않는 밋업 나가기 실패 - 404 Not Found")
    void leaveMeetup_nonExistentMeetup_fail() {
        // Given: 존재하지 않는 밋업 ID
        String nonExistentMeetupId = "00000000-0000-0000-0000-000000000000";
        
        // When & Then: 존재하지 않는 밋업에서 나가기 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", nonExistentMeetupId)
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("미인증 사용자 밋업 나가기 실패 - 401 Unauthorized")
    void leaveMeetup_unauthenticated_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 인증 토큰 없이 나가기 시도
        given()
                .pathParam("meetupId", meetup.id())
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(401); // Unauthorized
    }

    @Test
    @DisplayName("밋업 호스트 나가기 실패 - 400 Bad Request")
    void leaveMeetup_host_fail() {
        // Given: 밋업 생성 (호스트)
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 호스트가 자신의 밋업에서 나가기 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(400); // Bad Request - 호스트는 나갈 수 없음
    }

    @Test
    @DisplayName("밋업 나간 후 재참가 성공 - 200 OK")
    void leaveMeetup_rejoinAfterLeave_success() {
        // Given: 밋업 생성 및 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200);
        
        // When: 밋업 나가기
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .delete("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(204);
        
        // Then: 다시 참가 가능
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("profile.nickname", equalTo(profiles.get(1).getNickname()))
                .body("role", equalTo("MEMBER"));
    }

    @Test
    @DisplayName("동시 여러 사용자 나가기 성공 - 204 No Content")
    void leaveMeetup_multipleUsers_success() {
        // Given: 밋업 생성 및 여러 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        for (int i = 1; i < TEST_USER_COUNT; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens.get(i).accessToken())
                    .pathParam("meetupId", meetup.id())
            .when()
                    .post("/{meetupId}/participants")
            .then()
                    .statusCode(200);
        }
        
        // When: 여러 사용자가 밋업을 나감 (호스트 제외)
        for (int i = 1; i < TEST_USER_COUNT; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens.get(i).accessToken())
                    .pathParam("meetupId", meetup.id())
            .when()
                    .delete("/{meetupId}/participants")
            .then()
                    .statusCode(204);
        }
        
        // Then: 호스트만 남아있는지 확인
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].profile.nickname", equalTo(profiles.get(0).getNickname()))
                .body("[0].role", equalTo("HOST"));
    }
}