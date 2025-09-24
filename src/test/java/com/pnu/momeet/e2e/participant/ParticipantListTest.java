package com.pnu.momeet.e2e.participant;

import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("participant")
@DisplayName("E2E : 참가자 목록 조회 테스트")
public class ParticipantListTest extends BaseParticipantTest {


    @Test
    @DisplayName("참가자 목록 조회 성공 - 200 OK")
    void getParticipantsList_success() {
        // Given: 밋업 생성 및 여러 사용자 참가
        MeetupDetail meetup = createTestMeetup(0);
        
        // 다른 사용자들을 밋업에 참가시킴
        for (int i = 1; i < 3; i++) {
            given()
                    .header("Authorization", "Bearer " + tokens.get(i).accessToken())
                    .pathParam("meetupId", meetup.id())
            .when()
                    .post("/{meetupId}/participants")
            .then()
                    .statusCode(200);
        }
        
        // When & Then: 참가자 목록 조회
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(3)) // 호스트 + 참가자 2명
                .body("profile.nickname", hasItems(
                        profiles.getFirst().getNickname(),
                        profiles.get(1).getNickname(),
                        profiles.get(2).getNickname()
                ))
                .body("find { it.role == 'HOST' }.profile.nickname", equalTo(profiles.getFirst().getNickname()))
                .body("findAll { it.role == 'MEMBER' }.size()", equalTo(2));
    }

    @Test
    @DisplayName("빈 참가자 목록 조회 성공 - 200 OK")
    void getParticipantsList_emptyList_success() {
        // Given: 밋업 생성 (호스트만 있음)
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 참가자 목록 조회
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(1)) // 호스트만
                .body("[0].profile.nickname", equalTo(profiles.getFirst().getNickname()))
                .body("[0].role", equalTo("HOST"));
    }

    @Test
    @DisplayName("일반 참가자 목록 조회 성공 - 200 OK")
    void getParticipantsList_regularParticipant_success() {
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
        
        // When & Then: 일반 참가자가 목록 조회
        given()
                .header("Authorization", "Bearer " + tokens.get(1).accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @DisplayName("존재하지 않는 밋업 참가자 목록 조회 실패 - 404 Not Found")
    void getParticipantsList_nonExistentMeetup_fail() {
        // Given: 존재하지 않는 밋업 ID
        String nonExistentMeetupId = "00000000-0000-0000-0000-000000000000";
        
        // When & Then: 존재하지 않는 밋업의 참가자 목록 조회
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", nonExistentMeetupId)
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(404); // Not Found
    }

    @Test
    @DisplayName("미인증 사용자 참가자 목록 조회 실패 - 401 Unauthorized")
    void getParticipantsList_unauthenticated_fail() {
        // Given: 밋업 생성
        MeetupDetail meetup = createTestMeetup(0);
        
        // When & Then: 인증 토큰 없이 목록 조회
        given()
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(401); // Unauthorized
    }

    @Test
    @DisplayName("참가자 프로필 정보 포함 확인 - 200 OK")
    void getParticipantsList_profileInfoIncluded_success() {
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
        
        // When & Then: 참가자 목록 조회 시 프로필 정보 확인
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].profile", notNullValue())
                .body("[0].profile.nickname", notNullValue())
                .body("[0].profile.age", notNullValue())
                .body("[0].profile.gender", notNullValue())
                .body("[0].createdAt", notNullValue())
                .body("[0].isRated", notNullValue());
    }

    @Test
    @DisplayName("다수 참가자 목록 조회 성공 - 200 OK")
    void getParticipantsList_multipleParticipants_success() {
        // Given: 밋업 생성 및 모든 테스트 사용자 참가
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
        
        // When & Then: 전체 참가자 목록 조회
        given()
                .header("Authorization", "Bearer " + tokens.getFirst().accessToken())
                .pathParam("meetupId", meetup.id())
        .when()
                .get("/{meetupId}/participants")
        .then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(TEST_USER_COUNT))
                .body("findAll { it.role == 'HOST' }.size()", equalTo(1))
                .body("findAll { it.role == 'MEMBER' }.size()", equalTo(TEST_USER_COUNT - 1));
    }
}