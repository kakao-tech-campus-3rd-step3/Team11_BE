package com.pnu.momeet.e2e.participant;

import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("participant")
@DisplayName("E2E : 밋업 참가 테스트")
public class ParticipantJoinTest extends BaseParticipantTest {


    @Test
    @DisplayName("밋업 참가 성공 - 200 OK")
    void joinMeetup_success() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 다른 사용자가 밋업에 참가
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("profile.nickname", equalTo(profiles.get(1).getNickname()))
                .body("role", equalTo("MEMBER"))
                .body("isRated", equalTo(false));
    }

    @Test
    @DisplayName("여러 사용자 밋업 참가 성공 - 200 OK")
    void joinMeetup_multipleUsers_success() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 여러 사용자가 순차적으로 참가
        for (int i = 1; i < TEST_USER_COUNT; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens.get(i).accessToken())
                    .pathParam("meetupId", meetup.id())
            .when()
                    .post("/{meetupId}/participants")
            .then()
                    .log().all()
                    .statusCode(200)
                    .body("profile.nickname", equalTo(profiles.get(i).getNickname()))
                    .body("role", equalTo("MEMBER"));
        }
    }

    @Test
    @DisplayName("중복 참가 실패 - 409 Conflict")
    void joinMeetup_duplicate_fail() {
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
        
        // When & Then: 같은 사용자가 다시 참가 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(409); // Conflict
    }

    @Test
    @DisplayName("존재하지 않는 밋업 참가 실패 - 404 Not Found")
    void joinMeetup_nonExistentMeetup_fail() {
        // Given: 존재하지 않는 밋업 ID
        String nonExistentMeetupId = "00000000-0000-0000-0000-000000000000";
        
        // When & Then: 존재하지 않는 밋업에 참가 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", nonExistentMeetupId)
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("미인증 사용자 참가 실패 - 401 Unauthorized")
    void joinMeetup_unauthenticated_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 인증 토큰 없이 참가 시도
        given()
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(401); // Unauthorized
    }

    @Test
    @DisplayName("잘못된 토큰으로 참가 실패 - 401 Unauthorized")
    void joinMeetup_invalidToken_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 잘못된 토큰으로 참가 시도
        given()
                .header("Authorization", "Bearer invalid_token")
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(401); // Unauthorized
    }

    @Test
    @DisplayName("밋업 호스트 자동 참가 확인 - 200 OK")
    void joinMeetup_hostAutoParticipation_success() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 참가자 목록 조회 시 호스트가 HOST 역할로 포함되어 있음
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