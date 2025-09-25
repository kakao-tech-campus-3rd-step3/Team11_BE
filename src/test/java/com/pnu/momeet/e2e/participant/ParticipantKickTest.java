package com.pnu.momeet.e2e.participant;

import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("participant")
@DisplayName("E2E : 참가자 강퇴 테스트")
public class ParticipantKickTest extends BaseParticipantTest {


    @Test
    @DisplayName("참가자 강퇴 성공 - 204 No Content")
    void kickParticipant_success() {
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
        
        // When & Then: 호스트가 참가자를 강퇴
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(204); // No Content
        
        // 참가자 목록에서 제거되었는지 확인
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(1)) // 호스트만 남음
                .body("[0].profile.nickname", equalTo(profiles.getFirst().getNickname()))
                .body("profile.nickname", not(hasItem(profiles.get(1).getNickname())));
    }

    @Test
    @DisplayName("특정 참가자만 강퇴 성공 - 204 No Content")
    void kickParticipant_specific_success() {
        // Given: 밋업 생성 및 여러 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 세 명의 사용자가 밋업에 참가
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
        
        given()
                .header("Authorization", "Bearer " + tokens.get(3).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .post("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200);
        
        Long participantToKickId = joinResponse.jsonPath().getLong("id");
        
        // When: 호스트가 특정 참가자를 강퇴
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantToKickId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(204);
        
        // Then: 강퇴된 참가자만 제거되고 나머지는 유지
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(3)) // 호스트 + 2명 참가자
                .body("profile.nickname", hasItems(
                        profiles.getFirst().getNickname(),
                        profiles.get(1).getNickname(),
                        profiles.get(3).getNickname()
                ))
                .body("profile.nickname", not(hasItem(profiles.get(2).getNickname())));
    }

    @Test
    @DisplayName("일반 참가자 강퇴 시도 실패 - 403 Forbidden")
    void kickParticipant_regularParticipant_fail() {
        // Given: 밋업 생성 및 여러 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
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
        
        // When & Then: 일반 참가자가 다른 참가자를 강퇴 시도
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(403); // Forbidden - 권한 없음
    }

    @Test
    @DisplayName("존재하지 않는 참가자 강퇴 실패 - 404 Not Found")
    void kickParticipant_nonExistent_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        Long nonExistentParticipantId = 99999L;
        
        // When & Then: 존재하지 않는 참가자를 강퇴 시도
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", nonExistentParticipantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("호스트 자신 강퇴 실패 - 400 Bad Request")
    void kickParticipant_hostSelf_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // 호스트의 참가자 ID 조회
        Response participantsResponse = given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .extract().response();
        
        Long hostParticipantId = participantsResponse.jsonPath().getLong("find { it.role == 'HOST' }.id");
        
        // When & Then: 호스트가 자신을 강퇴 시도
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", hostParticipantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(400); // Bad Request - 호스트는 자신을 강퇴할 수 없음
    }

    @Test
    @DisplayName("존재하지 않는 밋업 강퇴 실패 - 404 Not Found")
    void kickParticipant_nonExistentMeetup_fail() {
        // Given: 존재하지 않는 밋업 ID
        String nonExistentMeetupId = "00000000-0000-0000-0000-000000000000";
        Long participantId = 1L;
        
        // When & Then: 존재하지 않는 밋업에서 강퇴 시도
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", nonExistentMeetupId)
                .pathParam("participantId", participantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("미인증 사용자 강퇴 실패 - 401 Unauthorized")
    void kickParticipant_unauthenticated_fail() {
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
        
        // When & Then: 인증 토큰 없이 강퇴 시도
        given()
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(401); // Unauthorized
    }

    @Test
    @DisplayName("새 호스트 강퇴 권한 확인 - 204 No Content")
    void kickParticipant_newHostCanKick_success() {
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
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId1)
        .when()
                .patch("/{meetupId}/participants/{participantId}/grant")
        .then()
                .log().all()
                .statusCode(200);
        
        // When & Then: 새로 호스트가 된 사용자가 다른 참가자를 강퇴
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId2)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(204);
        
        // 강퇴된 참가자가 목록에서 제거되었는지 확인
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(2)) // 원래 호스트 + 새 호스트
                .body("profile.nickname", not(hasItem(profiles.get(2).getNickname())));
    }

    @Test
    @DisplayName("강퇴된 참가자 재참가 가능 - 200 OK")
    void kickParticipant_rejoinAfterKick_success() {
        // Given: 밋업 생성, 사용자 참가 및 강퇴
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
        
        // 참가자 강퇴
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
                .pathParam("participantId", participantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(204);
        
        // When & Then: 강퇴된 사용자가 다시 참가
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
    @DisplayName("다른 밋업 참가자 강퇴 실패 - 404 Not Found")
    void kickParticipant_differentMeetup_fail() {
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
        
        // When & Then: 첫 번째 밋업의 호스트가 두 번째 밋업의 참가자를 강퇴 시도
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup1.id())
                .pathParam("participantId", participantId)
        .when()
                .delete("/{meetupId}/participants/{participantId}")
        .then()
                .log().all()
                .statusCode(404); // Not Found - 해당 밋업의 참가자가 아님
    }
}