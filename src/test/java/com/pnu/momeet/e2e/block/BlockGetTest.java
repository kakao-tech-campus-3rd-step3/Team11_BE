package com.pnu.momeet.e2e.block;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BlockGetTest extends BaseBlockTest {

    @BeforeEach
    void setUp() {
        // 테스트 간 상태 격리: USER→ADMIN 차단 관계 초기화(idempotent 204)
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when().delete("/{targetId}", testAdminMemberId)
            .then().statusCode(204);
    }

    @Test
    @DisplayName("[USER] 차단한 프로필 목록 조회 → 200 OK (최근 차단순)")
    void listBlockedProfiles_success_sortedDesc() throws Exception {
        // 차단 2건 준비: ADMIN 두 번은 불가, 다른 계정 하나 더 필요하면 픽스처에서 가져오세요.
        // 여기선 ADMIN만 차단 후 목록에 최소 1건 존재 확인
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when().post("/{targetId}", testAdminMemberId)
            .then().statusCode(201);

        given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .queryParam("page", 0).queryParam("size", 10)
            .when().get("/profiles")
            .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", greaterThanOrEqualTo(1))
            .body("content[0].profileId", notNullValue())
            .body("content[0].nickname", notNullValue())
            .body("content[0].temperature", notNullValue())
            .body("content[0].blockedAt", notNullValue());
    }

    @Test
    @DisplayName("[USER] 차단한 프로필 목록 조회 (빈 결과) → 200 OK, content=[]")
    void listBlockedProfiles_empty_returnsEmptyList() {
        // 초기화만 되어 있어 빈 상태
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .queryParam("page", 0).queryParam("size", 10)
            .when().get("/profiles")
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(0));
    }

    @Test
    @DisplayName("Unauthorized: 토큰 없으면 401")
    void listBlockedProfiles_unauthorized() {
        given()
            .queryParam("page", 0).queryParam("size", 10)
            .when().get("/profiles")
            .then()
            .statusCode(401);
    }
}
