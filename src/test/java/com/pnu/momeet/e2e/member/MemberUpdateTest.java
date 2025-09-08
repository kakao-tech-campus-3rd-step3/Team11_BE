package com.pnu.momeet.e2e.member;

import com.pnu.momeet.domain.member.dto.request.AdminChangePasswordRequest;
import com.pnu.momeet.domain.member.dto.request.ChangePasswordRequest;
import com.pnu.momeet.domain.member.dto.request.MemberEditRequest;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@Tag("update")
@DisplayName("E2E : Member 수정 테스트")
public class MemberUpdateTest extends BaseMemberTest {

    Member testMember = null;
    String testMemberToken = null;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        testMember = memberService.saveMember(new Member(
                "update_info_test@test.com",
                "updateTestPass1!",
                List.of(Role.ROLE_USER)
        ));
        testMemberToken = emailAuthService.login(
                testMember.getEmail(),
                 "updateTestPass1!"
        ).accessToken();

        toBeDeleted.add(testMember.getId());
    }

    @Test
    @DisplayName("멤버 정보 수정 성공 - 200 OK (비밀번호 제외)")
    public void update_member_info_success() {

        // 정보 수정 요청 (비밀번호 제외)
        MemberEditRequest editRequest = new MemberEditRequest(
                List.of("ROLE_USER", "ROLE_ADMIN"),
                false,
                false
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(editRequest)
            .when()
                .put("/{id}", testMember.getId())
            .then()
                .log().all()
                .statusCode(200)
                .body("roles", hasItems("ROLE_USER", "ROLE_ADMIN"))
                .body("enabled", equalTo(false))
                .body("isAccountNonLocked", equalTo(false));
    }

    @Test
    @DisplayName("멤버 정보 수정 실패 - 404 Not Found (존재하지 않는 멤버)")
    public void update_member_info_fail_not_found() {
        MemberEditRequest editRequest = new MemberEditRequest(
                List.of("ROLE_USER"),
                true,
                true
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(editRequest)
            .when()
                .put("/" + java.util.UUID.randomUUID())
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("멤버 정보 수정 실패 - 400 Bad Request (잘못된 입력)")
    public void update_member_info_fail_bad_request() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(Map.of("roles", List.of("INVALID_ROLE")))
            .when()
                .put("/" + java.util.UUID.randomUUID())
            .then()
                .log().all()
                .statusCode(400)
                .body("validationErrors", not(empty()));
    }

    @Test
    @DisplayName("멤버 정보 수정 실패 - 401 Unauthorized (인증 실패)")
    public void update_member_info_fail_unauthorized() {
        MemberEditRequest editRequest = new MemberEditRequest(
                List.of("ROLE_USER"),
                true,
                true
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .contentType(ContentType.JSON)
                .body(editRequest)
            .when()
                .put("/" + java.util.UUID.randomUUID())
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("멤버 정보 수정 실패 - 403 Forbidden (권한 없음)")
    public void update_member_info_fail_forbidden() {
        MemberEditRequest editRequest = new MemberEditRequest(
                List.of("ROLE_USER"),
                true,
                true
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
                .contentType(ContentType.JSON)
                .body(editRequest)
            .when()
                .put("/{id}", testMember.getId())
            .then()
                .log().all()
                .statusCode(403);
    }

    // 비밀번호 변경 테스트 (본인)
    @Test
    @DisplayName("본인 비밀번호 변경 성공 - 200 OK")
    public void change_own_password_success() {

        ChangePasswordRequest req = new ChangePasswordRequest(
                "updateTestPass1!",
                "newPassword123!"
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + testMemberToken)
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/me/password")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(testMember.getId().toString()),
                    "email", equalTo(testMember.getEmail()),
                    "roles", hasItem("ROLE_USER")
                );
    }

    @Test
    @DisplayName("본인 비밀번호 변경 실패 - 400 Bad Request (기존 비밀번호 불일치)")
    public void change_own_password_fail_wrong_old() {
        ChangePasswordRequest req = new ChangePasswordRequest(
                "wrongOldPassword1!",
                "newPassword123!"
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/me/password")
            .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("본인 비밀번호 변경 실패 - 401 Unauthorized (인증 실패)")
    public void change_own_password_fail_unauthorized() {
        ChangePasswordRequest req = new ChangePasswordRequest(
                TEST_USER_PASSWORD,
                "newPassword123!"
        );
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/me/password")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("관리자에 의한 비밀번호 변경 성공 - 200 OK")
    public void admin_change_password_success() {
        AdminChangePasswordRequest req = new AdminChangePasswordRequest("newAdminPassword123!");
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/{id}/password", testMember.getId())
            .then()
                .statusCode(200)
                .body(
                    "id", equalTo(testMember.getId().toString()),
                    "email", equalTo(testMember.getEmail()),
                    "roles", hasItem("ROLE_USER")
                );
    }

    @Test
    @DisplayName("관리자에 의한 비밀번호 변경 실패 - 400 Bad Request (비밀번호 유효성)")
    public void admin_change_password_fail_bad_request() {
        AdminChangePasswordRequest req = new AdminChangePasswordRequest("short");
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/{id}/password", testMember.getId())
            .then()
                .statusCode(400)
                .body("validationErrors", not(empty()));
    }

    @Test
    @DisplayName("관리자에 의한 비밀번호 변경 실패 - 401 Unauthorized (인증 실패)")
    public void admin_change_password_fail_unauthorized() {
        AdminChangePasswordRequest req = new AdminChangePasswordRequest("newAdminPassword123!");
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/" + java.util.UUID.randomUUID() + "/password")
            .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("관리자에 의한 비밀번호 변경 실패 - 403 Forbidden (권한 없음)")
    public void admin_change_password_fail_forbidden() {
        AdminChangePasswordRequest req = new AdminChangePasswordRequest("newAdminPassword123!");
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .put("/" + java.util.UUID.randomUUID() + "/password")
            .then()
                .statusCode(403);
    }
}
