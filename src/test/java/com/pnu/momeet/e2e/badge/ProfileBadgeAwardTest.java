package com.pnu.momeet.e2e.badge;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProfileBadgeAwardTest extends BaseProfileBadgeTest {

    @Test
    @DisplayName("관리자 - 배지 단건 수동 부여 성공(신규) → 200 & 응답 필드 검증")
    void award_success_firstTime() {
        // 테스트용 배지 한 개 생성
        Badge badge = badgeRepository.save(
            Badge.create(
                "E2E 신규 배지",
                "E2E 테스트용",
                "https://icon/e2e.png",
                "E2E_NEW_CODE"
            ));
        badgesToBeDeleted.add(badge.getId());

        // 관리자 토큰
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        // 수동 부여 호출
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .contentType(ContentType.JSON)
            .body("{\"code\":\"" + badge.getCode() + "\"}")
            .when()
            .post("/{profileId}/badges/award", testUserProfileId)
            .then()
            .statusCode(201)
            .body("targetProfileId", equalTo(testUserProfileId.toString()))
            .body("badgeId", notNullValue())
            .body("badgeName", equalTo("E2E 신규 배지"))
            .body("badgeDescription", notNullValue())
            .body("badgeIconUrl", notNullValue())
            .body("badgeCode", equalTo("E2E_NEW_CODE"))
            .body("profileBadgeCreatedAt", notNullValue());
    }

    @Test
    @DisplayName("관리자 - 배지 단건 수동 부여 중복 → 409(CONFLICT) (UNIQUE 제약 전역 핸들러)")
    void award_conflict_alreadyOwned() {
        // 사전 조건 - 유저가 이미 가진 배지를 하나 확보하기 위해 목록 조회 -> 첫 번째 배지 코드 추출
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        String ownedCode =
            given()
                .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .queryParam("sort", "representative,DESC,createdAt,DESC")
                .when()
                .get("/{profileId}/badges", testUserProfileId)
                .then()
                .statusCode(200)
                .extract().path("content[0].code");

        assertNotNull(ownedCode, "사전 데이터에 최소 1개의 프로필 배지가 존재해야 합니다.");

        // 동일 코드로 다시 수여 시도 -> UNIQUE_PROFILE_BADGE 전역 핸들러가 409로 매핑
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .contentType(ContentType.JSON)
            .body("{\"code\":\"" + ownedCode + "\"}")
            .when()
            .post("/{profileId}/badges/award", testUserProfileId)
            .then()
            .statusCode(409)
            .body("detail", equalTo("이미 보유한 배지입니다."));
    }

    @Test
    @DisplayName("관리자 - 존재하지 않는 배지 코드 → 404")
    void award_badgeNotFound_404() {
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .contentType(ContentType.JSON)
            .body("{\"code\":\"NOT_EXIST_CODE\"}")
            .when()
            .post("/{profileId}/badges/award", testUserProfileId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("관리자 - 존재하지 않는 프로필 → 404")
    void award_profileNotFound_404() {
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();
        UUID notExistProfileId = UUID.randomUUID();

        given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .contentType(ContentType.JSON)
            .body("{\"code\":\"E2E_SOME_CODE\"}")
            .when()
            .post("/{profileId}/badges/award", notExistProfileId)
            .then()
            .statusCode(404);
    }
}
