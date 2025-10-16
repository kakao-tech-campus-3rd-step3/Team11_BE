package com.pnu.momeet.e2e.report;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ReportGetTest extends BaseAdminReportTest {

    @Test
    @DisplayName("ADMIN - OPEN 신고 전체 조회 (최신순, 페이지)")
    void admin_getOpenReports_paged_desc() throws Exception {
        // given: 서로 다른 reporter로 2건 생성(쿨타임 룰 회피)
        var userToken  = getToken(Role.ROLE_USER).accessToken();
        var adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        // USER -> ADMIN
        UUID r1 = UUID.fromString(
            given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + userToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("targetProfileId", testAdminProfileId.toString())
                .multiPart("category", "ABUSE")
                .multiPart("detail", "욕설")
                .when().post("/api/reports")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract().jsonPath().getString("reportId")
        );
        reportsToBeDeleted.add(r1);

        // createdAt 순서 안정화
        Thread.sleep(15);

        // ADMIN -> USER
        UUID r2 = UUID.fromString(
            given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("targetProfileId", testUserProfileId.toString())
                .multiPart("category", "SPAM")
                .multiPart("detail", "광고")
                .when().post("/api/reports")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .extract().jsonPath().getString("reportId")
        );
        reportsToBeDeleted.add(r2);

        // when: ADMIN이 OPEN 목록 조회(절대경로 호출: /api/admin/reports)
        ExtractableResponse<Response> res = given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .contentType(ContentType.URLENC)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when().get("/api/admin/reports")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", greaterThanOrEqualTo(2))
            .extract();

        // then: 최신순 확인 (두 번째로 생성한 r2가 첫 번째)
        String firstId  = res.jsonPath().getString("content[0].reportId");
        String secondId = res.jsonPath().getString("content[1].reportId");
        org.junit.jupiter.api.Assertions.assertEquals(r2.toString(), firstId);
        org.junit.jupiter.api.Assertions.assertEquals(r1.toString(), secondId);
    }

    @Test
    @DisplayName("USER - 관리자 목록 접근 불가 403")
    void user_cannot_access_admin_open_list_403() {
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when().get("/api/admin/reports")
            .then().log().all()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("미인증 - 401")
    void no_token_401() {
        given().log().all()
            .when().get("/api/admin/reports")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("ADMIN - 신고 단건 조회 200 (상세 필드 포함)")
    void admin_getReport_200() {
        // given: USER가 신고 1건 생성 (POST는 /api/reports)
        var userToken  = getToken(Role.ROLE_USER).accessToken();
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

        // when/then: ADMIN이 단건 조회 (GET /api/admin/reports/{id})
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when().get("/api/admin/reports/{id}", reportId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("reportId", equalTo(reportId.toString()))
            .body("reporterProfileId", equalTo(testUserProfileId.toString()))
            .body("targetProfileId", equalTo(testAdminProfileId.toString()))
            .body("detail", notNullValue())     // 상세 존재
            .body("images", notNullValue())     // 첨부 배열(비어있을 수 있음)
            .body("createdAt", notNullValue()); // TIMESTAMP(LocalDateTime) 존재만 체크
    }

    @Test
    @DisplayName("USER - 관리자 단건 조회 접근 불가 403")
    void user_cannot_access_admin_get_403() {
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when().get("/api/admin/reports/{id}", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("미인증 - 401")
    void no_token_report_401() {
        given().log().all()
            .when().get("/api/admin/reports/{id}", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("ADMIN - 존재하지 않는 신고 404")
    void admin_notFound_404() {
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when().get("/api/admin/reports/{id}", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
