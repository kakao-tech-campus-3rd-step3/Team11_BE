package com.pnu.momeet.e2e.block;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BlockCreateTest extends BaseBlockTest{

    @BeforeEach
    void cleanBlockRelation() {
        // USER ↔ ADMIN 사이 차단 상태를 항상 비워두기 (idempotent DELETE)
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{targetProfileId}", testAdminProfileId)
            .then()
            .statusCode(anyOf(is(204), is(401))); // 미로그인 베이스면 204만 체크해도 OK
    }

    @Test
    @DisplayName("[USER] 차단 생성 성공 → 201 Created")
    void createBlock_success_201() {
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .post("/{targetProfileId}", testAdminProfileId) // USER가 ADMIN을 차단
            .then()
            .log().all()
            .statusCode(201)
            .body("blockedProfileId", equalTo(testAdminProfileId.toString()))
            .body("blockerProfileId", equalTo(testUserProfileId.toString()))
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("[USER] 자기 자신 차단 → 400 Bad Request")
    void createBlock_self_400() {
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .post("/{targetProfileId}", testUserProfileId)
            .then()
            .log().all()
            .statusCode(400);
    }

    @Test
    @DisplayName("[USER] 중복 차단 → 409 Conflict")
    void createBlock_duplicate_409() {
        // 선차단
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .post("/{targetProfileId}", testAdminProfileId)
            .then().statusCode(201);

        // 동일 대상 재요청
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .post("/{targetProfileId}", testAdminProfileId)
            .then()
            .log().all()
            .statusCode(409);
    }
}
