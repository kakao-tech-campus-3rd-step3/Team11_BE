package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Tag("state")
@DisplayName("E2E : Meetup 상태 변경 테스트")
class MeetupStateTest extends BaseMeetupTest {

    @Autowired
    private MeetupRepository meetupRepository;
    
    @Autowired
    private SigunguEntityService sigunguEntityService;
    
    @Autowired
    private GeometryFactory geometryFactory;

    @Test
    @DisplayName("모임 시작 성공 테스트 - 204 No Content")
    void start_meetup_success() {
        // Given: OPEN 상태의 모임 생성
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.OPEN);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .post("/me/start")
            .then()
                .log().all()
                .statusCode(204);
        
        // 상태 변경 확인
        Meetup updatedMeetup = meetupRepository.findById(meetupId).orElseThrow();
        assert updatedMeetup.getStatus() == MeetupStatus.IN_PROGRESS;
    }

    @Test
    @DisplayName("모임 시작 실패 테스트 - 404 Not Found - 시작 가능한 모임 없음")
    void start_meetup_fail_no_available_meetup() {
        // Given: OPEN 상태의 모임이 없는 상태
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .post("/me/start")
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("모임 시작 실패 테스트 - 401 Unauthorized")
    void start_meetup_fail_unauthorized() {
        // Given: OPEN 상태의 모임 생성
        createTestMeetup(ALICE_EMAIL, MeetupStatus.OPEN);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .post("/me/start")
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("모임 취소 성공 테스트 - 204 No Content")
    void cancel_meetup_success() {
        // Given: OPEN 상태의 모임 생성
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.OPEN);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .post("/me/cancel")
            .then()
                .log().all()
                .statusCode(204);
        
        // 상태 변경 확인
        Meetup updatedMeetup = meetupRepository.findById(meetupId).orElseThrow();
        assert updatedMeetup.getStatus() == MeetupStatus.CANCELED;
    }

    @Test
    @DisplayName("모임 취소 실패 테스트 - 404 Not Found - 취소 가능한 모임 없음")
    void cancel_meetup_fail_no_available_meetup() {
        // Given: OPEN 상태의 모임이 없는 상태
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .post("/me/cancel")
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("관리자 모임 취소 성공 테스트 - OPEN 상태 - 204 No Content")
    void cancel_meetup_admin_success_open() {
        // Given: OPEN 상태의 모임 생성
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.OPEN);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .post("/{meetupId}/cancel", meetupId)
            .then()
                .log().all()
                .statusCode(204);
        
        // 상태 변경 확인
        Meetup updatedMeetup = meetupRepository.findById(meetupId).orElseThrow();
        assert updatedMeetup.getStatus() == MeetupStatus.CANCELED;
    }

    @Test
    @DisplayName("관리자 모임 취소 성공 테스트 - IN_PROGRESS 상태 - 204 No Content")
    void cancel_meetup_admin_success_in_progress() {
        // Given: IN_PROGRESS 상태의 모임 생성
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.IN_PROGRESS);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .post("/{meetupId}/cancel", meetupId)
            .then()
                .log().all()
                .statusCode(204);
        
        // 상태 변경 확인
        Meetup updatedMeetup = meetupRepository.findById(meetupId).orElseThrow();
        assert updatedMeetup.getStatus() == MeetupStatus.CANCELED;
    }

    @Test
    @DisplayName("관리자 모임 취소 실패 테스트 - 409 Conflict - 취소 불가능한 상태")
    void cancel_meetup_admin_fail_invalid_status() {
        // Given: ENDED 상태의 모임 생성 (취소 불가능한 상태)
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.ENDED);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .post("/{meetupId}/cancel", meetupId)
            .then()
                .log().all()
                .statusCode(409);
    }

    @Test
    @DisplayName("관리자 모임 취소 실패 테스트 - 403 Forbidden - 권한 없음")
    void cancel_meetup_admin_fail_forbidden() {
        // Given: OPEN 상태의 모임 생성
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.OPEN);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
                .post("/{meetupId}/cancel", meetupId)
            .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("관리자 모임 취소 실패 테스트 - 404 Not Found - 존재하지 않는 모임")
    void cancel_meetup_admin_fail_not_found() {
        // Given: 존재하지 않는 모임 ID
        UUID nonExistentMeetupId = UUID.randomUUID();
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
                .post("/{meetupId}/cancel", nonExistentMeetupId)
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("모임 종료 성공 테스트 - 204 No Content")
    void finish_meetup_success() {
        // Given: IN_PROGRESS 상태의 모임 생성
        UUID meetupId = createTestMeetup(ALICE_EMAIL, MeetupStatus.IN_PROGRESS);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .post("/me/finish")
            .then()
                .log().all()
                .statusCode(204);
        
        // 상태 변경 확인
        Meetup updatedMeetup = meetupRepository.findById(meetupId).orElseThrow();
        assert updatedMeetup.getStatus() == MeetupStatus.ENDED;
        assert updatedMeetup.getEndAt() != null;
    }

    @Test
    @DisplayName("모임 종료 실패 테스트 - 409 Conflict - 종료 가능한 모임 없음")
    void finish_meetup_fail_no_available_meetup() {
        // Given: IN_PROGRESS 상태의 모임이 없는 상태
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + userTokens.get(ALICE_EMAIL).accessToken())
            .when()
                .post("/me/finish")
            .then()
                .log().all()
                .statusCode(409);
    }

    @Test
    @DisplayName("모임 종료 실패 테스트 - 401 Unauthorized")
    void finish_meetup_fail_unauthorized() {
        // Given: IN_PROGRESS 상태의 모임 생성
        createTestMeetup(ALICE_EMAIL, MeetupStatus.IN_PROGRESS);
        
        // When & Then
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .post("/me/finish")
            .then()
                .log().all()
                .statusCode(401);
    }

    /**
     * 테스트용 모임을 생성합니다.
     * @param ownerEmail 모임 소유자 이메일
     * @param status 모임 상태
     * @return 생성된 모임 ID
     */
    private UUID createTestMeetup(String ownerEmail, MeetupStatus status) {
        // 좌표로 시군구 조회
        Point point = geometryFactory.createPoint(new Coordinate(129.08262659183725, 35.23203443995263));
        Sigungu sigungu = sigunguEntityService.getByPointIn(point);
        
        // 모임 생성
        Meetup meetup = Meetup.builder()
                .owner(userProfiles.get(ownerEmail))
                .name("테스트 모임 - " + status.name())
                .category(MainCategory.GAME)
                .subCategory(SubCategory.BOARD_GAME)
                .description("테스트용 모임입니다.")
                .capacity(6)
                .scoreLimit(35.0)
                .locationPoint(point)
                .address("부산광역시 금정구 부산대학로 63번길 2")
                .sigungu(sigungu)
                .endAt(LocalDateTime.now().plusHours(3))
                .status(status)
                .build();
        
        // 해시태그 설정
        meetup.setHashTags(List.of("보드게임", "친목"));
        
        Meetup savedMeetup = meetupRepository.save(meetup);
        toBeDeleted.add(savedMeetup.getId());
        
        return savedMeetup.getId();
    }
}
