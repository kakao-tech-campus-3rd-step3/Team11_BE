package com.pnu.momeet.e2e.report;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ReportProcessTest extends BaseAdminReportTest {

    @Test
    @DisplayName("ADMIN - 신고 처리 성공(OPEN→ENDED)")
    void admin_process_ok() {
        var userToken  = getToken(Role.ROLE_USER).accessToken();

        // 1) 신고 생성(POST /api/reports)
        UUID reportId = UUID.fromString(
            given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + userToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("targetProfileId", testAdminProfileId.toString())
                .multiPart("category", "ABUSE")
                .multiPart("detail", "욕설 신고")
                .when().post("/api/reports")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract().jsonPath().getString("reportId")
        );
        reportsToBeDeleted.add(reportId);

        // 2) ADMIN 처리(PATCH /api/admin/reports/{id}/process)
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body("{\"reply\":\"처리 완료\"}")
            .when().patch("/api/admin/reports/{id}/process", reportId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("reportId", equalTo(reportId.toString()))
            .body("status", equalTo("ENDED"))
            .body("adminReply", equalTo("처리 완료"))
            .body("processedBy", notNullValue())
            .body("processedAt", notNullValue());
    }

    @Test
    @DisplayName("ADMIN - 이미 ENDED 처리 시 409")
    void admin_process_conflict_409() {
        var userToken  = getToken(Role.ROLE_USER).accessToken();

        UUID reportId = UUID.fromString(
            given().header(AUTH_HEADER, BEAR_PREFIX + userToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("targetProfileId", testAdminProfileId.toString())
                .multiPart("category", "SPAM")
                .multiPart("detail", "광고")
                .when().post("/api/reports")
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().jsonPath().getString("reportId")
        );
        reportsToBeDeleted.add(reportId);

        // 1차 처리 OK
        given().header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body("{\"reply\":\"처리1\"}")
            .when().patch("/api/admin/reports/{id}/process", reportId)
            .then().statusCode(HttpStatus.OK.value())
            .body("status", equalTo("ENDED"));

        // 2차 처리 → 409
        given().header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body("{\"reply\":\"처리2\"}")
            .when().patch("/api/admin/reports/{id}/process", reportId)
            .then().statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("USER - 관리자 처리 API 접근 불가 403")
    void user_cannot_process_403() {
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body("{\"reply\":\"시도\"}")
            .when().patch("/api/admin/reports/{id}/process", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("미인증 - 401")
    void no_token_401() {
        given().log().all()
            .contentType(ContentType.JSON)
            .when().patch("/api/admin/reports/{id}/process", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("ADMIN - 존재하지 않는 신고 처리 시 404")
    void admin_not_found_404() {
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body("{\"reply\":\"처리\"}")
            .when().patch("/api/admin/reports/{id}/process", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
