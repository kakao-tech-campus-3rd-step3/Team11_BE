package com.pnu.momeet.e2e.badge;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pnu.momeet.domain.badge.service.ProfileBadgeEntityService;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProfileBadgeRepresentativeSetTest extends BaseProfileBadgeTest {

    @Autowired
    private ProfileBadgeEntityService entityService;

    @AfterEach
    void cleanup() {
        entityService.resetRepresentative(testUserProfileId);
    }

    @Test
    @DisplayName("내 대표 배지 설정 - 성공 (멱등 포함)")
    void setRepresentative_success() {
        // 1) 관리자 토큰으로 테스트 유저 배지 목록 조회 → badgeId 하나 추출
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        String badgeIdStr =
            given()
                .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sort", "representative,DESC,createdAt,DESC")
                .when()
                .get("/{profileId}/badges", testUserProfileId)
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .extract()
                .path("content[0].badgeId");

        UUID badgeId = UUID.fromString(badgeIdStr);

        assertNotNull(badgeId, "사전 데이터에 프로필 배지가 최소 1개 존재해야 합니다.");

        // 2) 유저 토큰으로 대표 배지 설정
        String userToken = getToken(Role.ROLE_USER).accessToken();

        given()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .contentType(ContentType.JSON)
            .body("{\"badgeId\":\"" + badgeId + "\"}")
            .when()
            .put("/me/badges/representative")
            .then()
            .statusCode(200)
            .body("badgeId", equalTo(badgeId.toString()))
            .body("representative", equalTo(true))
            .body("name", notNullValue())
            .body("code", notNullValue());

        // 3) 목록 재조회 시 해당 배지가 대표(true)인지 검증
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sort", "representative,DESC,createdAt,DESC")
            .when()
            .get("/{profileId}/badges", testUserProfileId)
            .then()
            .statusCode(200)
            .body("content.find { it.badgeId == '" + badgeId + "' }.representative", equalTo(true));
    }

    @Test
    @DisplayName("이미 대표인 배지를 다시 대표로 설정해도 200(멱등)")
    void setRepresentative_idempotent() {
        String userToken  = getToken(Role.ROLE_USER).accessToken();
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        // 임의의 badgeId 추출
        String badgeIdStr =
            given()
                .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sort", "representative,DESC,createdAt,DESC")
                .when().get("/{profileId}/badges", testUserProfileId)
                .then().statusCode(200)
                .extract().path("content[0].badgeId");

        UUID badgeId = UUID.fromString(badgeIdStr);

        // 1차 설정
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .contentType(ContentType.JSON)
            .body("{\"badgeId\":\"" + badgeId + "\"}")
            .when()
            .put("/me/badges/representative")
            .then()
            .statusCode(200)
            .body("representative", equalTo(true));

        // 2차(같은 배지 재설정) → 200 & representative=true
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .contentType(ContentType.JSON)
            .body("{\"badgeId\":\"" + badgeId + "\"}")
            .when()
            .put("/me/badges/representative")
            .then()
            .statusCode(200)
            .body("badgeId", equalTo(badgeId.toString()))
            .body("representative", equalTo(true));
    }
}
