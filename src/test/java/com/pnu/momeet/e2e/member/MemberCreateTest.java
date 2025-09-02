package com.pnu.momeet.e2e.member;

import com.pnu.momeet.domain.member.dto.MemberCreateRequest;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@Tag("create")
public class MemberCreateTest extends BaseMemberTest {

    @Test
    @DisplayName("멤버 생성 성공 테스트 - 201 Created")
    public void create_member_success() {
        MemberCreateRequest request = new MemberCreateRequest(
                "test1234@test.com",
                "testpass1234!",
                List.of("ROLE_USER")
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(201)
                .body(
                    "id", notNullValue(),
                    "email", equalTo(request.email()),
                    "provider", equalTo("EMAIL"),
                    "roles", hasItems("ROLE_USER"),
                    "enabled", equalTo(true),
                    "isAccountNonLocked", equalTo(true),
                    "createdAt", notNullValue(),
                    "updatedAt", notNullValue()
                );
    }

    @Test
    @DisplayName("멤버 생성 실패 테스트 - 400 Bad Request - 이메일 중복")
    public void create_member_fail_by_duplicate_email() {
        MemberCreateRequest request = new MemberCreateRequest(
                TEST_USER_EMAIL,
                "testpass1234!",
                List.of("ROLE_USER")
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("유효성 검사 실패 테스트 - 400 Bad Request - 잘못된 이메일 형식")
    public void create_member_fail_by_invalid_email_format() {
        List.of(
                Map.of(
                    "email", "invalid-email",
                    "password", "validPass123!",
                    "roles", List.of("ROLE_USER")
                ),
                Map.of(
                    "email", "testvalid@email.com",
                    "password", "invalid",
                    "roles", List.of("ROLE_USER")
                ),
                Map.of(
                    "email", "testvalid@email.com",
                    "password", "validPass123!"
                ),
                Map.of(
                    "email", "testvalid@email.com",
                    "password", "validPass123!",
                    "roles", List.of("INVALID_ROLE")
                )
        ).forEach(body ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                .when()
                    .post()
                .then()
                    .log().all()
                    .statusCode(400)
                    .body("validationErrors", not(empty()))
        );
    }

    @Test
    @DisplayName("멤버 생성 실패 테스트 - 401 Unauthorized - 인증 실패")
    public void create_member_fail_by_unauthorized() {
        MemberCreateRequest request = new MemberCreateRequest(
                "test1235@test.com",
                "testpass1234!",
                List.of("ROLE_USER")
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("멤버 생성 실패 테스트 - 403 Forbidden - 권한 없음")
    public void create_member_fail_by_forbidden() {
        MemberCreateRequest request = new MemberCreateRequest(
                "test1235@test.com",
                "testpass1234!",
                List.of("ROLE_USER")
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(403);
    }
}
