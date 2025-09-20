package com.pnu.momeet.e2e.participant;

import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("participant")
@DisplayName("E2E : 참가자 역할 권한 부여 테스트")
public class ParticipantRoleTest extends BaseParticipantTest {


    @Test
    @DisplayName("호스트 역할 부여 성공 - 200 OK")
    void grantHostRole_success() {
        // Given: 밋업 생성 및 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 사용자가 밋업에 참가
        Response joinResponse = given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Long participantId = joinResponse.jsonPath().getLong("id");
        
        // When & Then: 호스트가 참가자에게 호스트 역할 부여
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(200)
                .body("role", equalTo("HOST"))
                .body("profile.nickname", equalTo(profiles.get(1).getNickname()));
        
        // 참가자 목록에서 역할 변경 확인
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("findAll { it.role == 'HOST' }.size()", equalTo(1)) // HOST는 1명만 존재
                .body("find { it.profile.nickname == '" + profiles.get(1).getNickname() + "' }.role", equalTo("HOST")) // 새로운 HOST
                .body("find { it.profile.nickname == '" + profiles.get(0).getNickname() + "' }.role", equalTo("MEMBER")); // 기존 HOST는 MEMBER로 변경
    }

    @Test
    @DisplayName("일반 참가자 역할 부여 실패 - 403 Forbidden")
    void grantHostRole_regularParticipant_fail() {
        // Given: 밋업 생성 및 여러 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 두 사용자가 밋업에 참가
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200);
        
        Response joinResponse = given()
                .header("Authorization", "Bearer " + tokens.get(2).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Long participantId = joinResponse.jsonPath().getLong("id");
        
        // When & Then: 일반 참가자가 다른 참가자에게 역할 부여 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(403); // Forbidden - 권한 없음
    }

    @Test
    @DisplayName("존재하지 않는 참가자 역할 부여 실패 - 404 Not Found")
    void grantHostRole_nonExistentParticipant_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        Long nonExistentParticipantId = 99999L;
        
        // When & Then: 존재하지 않는 참가자에게 역할 부여 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", nonExistentParticipantId)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("존재하지 않는 밋업 역할 부여 실패 - 404 Not Found")
    void grantHostRole_nonExistentMeetup_fail() {
        // Given: 존재하지 않는 밋업 ID
        String nonExistentMeetupId = "00000000-0000-0000-0000-000000000000";
        Long participantId = 1L;
        
        // When & Then: 존재하지 않는 밋업에서 역할 부여 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", nonExistentMeetupId)
                .pathParam("participantId", participantId)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("미인증 사용자 역할 부여 실패 - 401 Unauthorized")
    void grantHostRole_unauthenticated_fail() {
        // Given: 밋업 생성 및 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        Response joinResponse = given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Long participantId = joinResponse.jsonPath().getLong("id");
        
        // When & Then: 인증 토큰 없이 역할 부여 시도
        given()
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(401); // Unauthorized
    }

    @Test
    @DisplayName("다른 밋업 참가자 역할 부여 실패 - 404 Not Found")
    void grantHostRole_differentMeetupParticipant_fail() {
        // Given: 두 개의 밋업 생성
        MeetupDetail meetup1 = createTestMeetup(0);
        MeetupDetail meetup2 = createTestMeetup(1);
        
        // 사용자가 두 번째 밋업에만 참가
        Response joinResponse = given()
                .header("Authorization", "Bearer " + tokens.get(2).accessToken())
                .pathParam("meetupId", meetup2.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Long participantId = joinResponse.jsonPath().getLong("id");
        
        // When & Then: 첫 번째 밋업의 호스트가 두 번째 밋업의 참가자에게 역할 부여 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup1.id())
                .pathParam("participantId", participantId)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(404); // Not Found - 해당 밋업의 참가자가 아님
    }

    @Test
    @DisplayName("새 호스트 역할 부여 권한 확인 - 200 OK")
    void grantHostRole_newHostCanGrant_success() {
        // Given: 밋업 생성 및 사용자들 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 두 사용자가 밋업에 참가
        Response joinResponse1 = given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Response joinResponse2 = given()
                .header("Authorization", "Bearer " + tokens.get(2).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Long participantId1 = joinResponse1.jsonPath().getLong("id");
        Long participantId2 = joinResponse2.jsonPath().getLong("id");
        
        // 첫 번째 사용자에게 호스트 역할 부여
        given()
                .header("Authorization", "Bearer " + tokens.get(0).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId1)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(200);
        
        // When & Then: 새로 호스트가 된 사용자가 다른 참가자에게 역할 부여
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId2)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(200)
                .body("role", equalTo("HOST"))
                .body("profile.nickname", equalTo(profiles.get(2).getNickname()));
    }
}