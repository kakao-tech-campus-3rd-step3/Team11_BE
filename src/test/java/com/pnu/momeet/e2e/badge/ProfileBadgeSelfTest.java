package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProfileBadgeSelfTest extends BaseProfileBadgeTest {

    @Test
    @DisplayName("내 배지 목록 조회 — 200 OK (기본 픽스처: user@test.com 은 1개, 대표 아님)")
    void getMyBadges_success() {

        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sort", "representative,desc,createdAt,desc")
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(200)
            // content 검증
            .body("content", notNullValue())
            .body("content.size()", greaterThanOrEqualTo(1))
            .body("content[0].name", equalTo("[TEST] 호감 인기인"))
            .body("content[0].description", equalTo("테스트용: 좋아요 10개"))
            .body("content[0].code", equalTo("LIKE_10"))
            .body("content[0].representative", equalTo(false))
            // page 메타
            .body("page.size", equalTo(10))
            .body("page.number", equalTo(0))
            .body("page.totalElements", greaterThanOrEqualTo(1))
            .body("page.totalPages", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("내 배지 목록 조회 — 200 OK (정렬 미지정: 저장소 기본 정렬 representative desc, createdAt desc)")
    void getMyBadges_success_defaultSort() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .queryParam("page", 0)
            .queryParam("size", 5)
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page.size", equalTo(5))
            .body("page.number", equalTo(0));
        // 정렬 자체는 DSL 기본(Order: representative desc -> createdAt desc)로 적용됨
    }

    @Test
    @DisplayName("내 배지 목록 조회 — 401 (인증 누락)")
    void getMyBadges_unauthorized() {
        RestAssured
            .given()
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/me/badges")
            .then()
            .log().all()
            .statusCode(401);
    }
}
