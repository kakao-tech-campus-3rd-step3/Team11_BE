package com.pnu.momeet.e2e.block;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BlockDeleteTest extends BaseBlockTest {

    @Test
    @DisplayName("[USER] 차단 해제 성공 → 204 (존재하는 차단)")
    void delete_existing_returns204() {
        // 선차단
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .post("/{targetId}", testAdminMemberId)
            .then()
            .statusCode(201);

        // 해제
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{targetId}", testAdminMemberId)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("[USER] 차단 해제 → 204 (존재하지 않아도 idempotent)")
    void delete_nonExisting_returns204() {
        // 사전에 없도록 보장 (idempotent)
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{targetId}", testAdminMemberId)
            .then()
            .statusCode(204);

        // 한 번 더 호출해도 204
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{targetId}", testAdminMemberId)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("Unauthorized: 토큰 없으면 401")
    void delete_unauthorized_returns401() {
        RestAssured.given()
            .when()
            .delete("/{targetId}", testAdminMemberId)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("[USER] 타인의 차단을 해제하려는 경우 → 204 (내 관계만 삭제 시도, 무해)")
    void delete_otherRelation_isHarmless() {
        // ADMIN이 USER를 차단(내 관계 아님)
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
            .post("/{targetId}", testUserMemberId)
            .then()
            .statusCode(201);

        // USER가 ADMIN 대상 해제 시도 → 내 관계가 아니므로 삭제 0건이지만 API는 204
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{targetId}", testAdminMemberId)
            .then()
            .statusCode(204);
    }
}
