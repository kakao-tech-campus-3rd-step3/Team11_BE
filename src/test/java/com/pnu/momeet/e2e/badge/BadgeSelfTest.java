package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BadgeSelfTest extends BaseBadgeTest {

    @Test
    @DisplayName("USER: 내 배지 조회 — [TEST] 호감 인기인 포함 & 대표 아님")
    void myBadges_user_containsLikeBadge_notRepresentative() {
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sort", "representative,DESC,createdAt,DESC")
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content[0].name", equalTo("[TEST] 호감 인기인"))
            .body("content[0].description", equalTo("테스트용: 좋아요 10개"))
            .body("content[0].code", equalTo("LIKE_10"))
            .body("content[0].representative", equalTo(false))
            .body("page.size", equalTo(10))
            .body("page.number", equalTo(0))
            .body("page.totalElements", equalTo(1))
            .body("page.totalPages",equalTo((1)));
    }

    @Test
    @DisplayName("ADMIN: 내 배지 조회 — [TEST] 모임 고수 포함 & 대표(true)가 첫번째로 온다")
    void myBadges_admin_containsProBadge_representativeFirst() {
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .queryParam("page", 0)
            .queryParam("size", 10)
            // 대표 우선 → 최신순
            .queryParam("sort", "representative,DESC,createdAt,DESC")
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content[0].name", equalTo("[TEST] 모임 고수"))
            .body("content[0].description", equalTo("테스트용: 10회 참여 배지"))
            .body("content[0].code", equalTo("TEN_JOINS"))
            .body("content[0].representative", equalTo(true))
            .body("content[1].name", equalTo("[TEST] 모임 새싹"))
            .body("content[1].description", equalTo("테스트용: 첫 참여 배지"))
            .body("content[1].code", equalTo("FIRST_JOIN"))
            .body("content[1].representative", equalTo(false))
            .body("page.size", equalTo(10))
            .body("page.number", equalTo(0))
            .body("page.totalElements", equalTo(2))
            .body("page.totalPages",equalTo((1)));
    }

    @Test
    @DisplayName("Unauthorized: 토큰 없으면 401")
    void myBadges_unauthorized_noToken() {
        RestAssured
            .given()
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(401);
    }

    @Test
    @DisplayName("Unauthorized: 잘못된 토큰이면 401")
    void myBadges_unauthorized_invalidToken() {
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + "invalid.token")
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(401);
    }
}