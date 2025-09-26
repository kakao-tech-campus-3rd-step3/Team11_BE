package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProfileBadgeGetTest extends BaseProfileBadgeTest {

    @Test
    @DisplayName("특정 사용자 배지 목록 조회 — USER 프로필 ID 기준")
    void getUserBadges_success() {

        // ROLE_ADMIN으로 로그인하여 ROLE_USER의 프로필을 조회
        String accessToken = getToken(Role.ROLE_ADMIN).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sort", "representative,DESC,createdAt,DESC")
            .when()
            .get("/{profileId}/badges", testUserProfileId)
            .then()
            .log().all()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content[0].name", equalTo("[TEST] 호감 인기인"))
            .body("content[0].description", equalTo("테스트용: 좋아요 10개"))
            .body("content[0].representative", equalTo(false))
            .body("content[0].code", equalTo("LIKE_10"))
            .body("page.size", equalTo(10))
            .body("page.number", equalTo(0))
            .body("page.totalElements", equalTo(1))
            .body("page.totalPages",equalTo((1)));
    }

    @Test
    @DisplayName("존재하지 않는 프로필 → 404")
    void getUserBadges_notFound() {

        String accessToken = getToken(Role.ROLE_ADMIN).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sort", "representative,DESC,createdAt,DESC")
            .when()
            .get("/{profileId}/badges", UUID.randomUUID())
            .then()
            .log().all()
            .statusCode(404);
    }
}
