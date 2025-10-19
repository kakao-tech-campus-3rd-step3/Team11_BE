package com.pnu.momeet.e2e.report;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReportSelfTest extends BaseReportTest {

    @Test
    @DisplayName("특정 신고 조회 - 신고자 본인 200")
    void getReport_success_owner() {
        // 1) 사전: USER가 신고 1건 생성(이미지 없이)
        var user = getToken(Role.ROLE_USER);
        ExtractableResponse<Response> created = given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + user.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("targetProfileId", testAdminProfileId.toString())
            .multiPart("category", "ABUSE")
            .multiPart("detail", "욕설")
            .when().post()
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .extract();

        UUID reportId = UUID.fromString(created.jsonPath().getString("reportId"));
        reportsToBeDeleted.add(reportId);

        // 2) 본인 토큰으로 단건 조회
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + user.accessToken())
            .when().get("/{reportId}", reportId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("reportId", equalTo(reportId.toString()))
            .body("reporterProfileId", equalTo(testUserProfileId.toString()))
            .body("targetProfileId", equalTo(testAdminProfileId.toString()))
            .body("category", equalTo("ABUSE"))
            .body("status", anyOf(equalTo("OPEN"), equalTo("ENDED")))
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("특정 신고 조회 - 토큰 없음 401")
    void getReport_fail_401() {
        given().log().all()
            .contentType(ContentType.URLENC.withCharset(StandardCharsets.UTF_8))
            .when().get("/{reportId}", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("특정 신고 조회 - 존재하지 않는 ID 404")
    void getReport_fail_404() {
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when().get("/{reportId}", UUID.randomUUID())
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("내 신고 목록 조회 - 최신순(createdAt DESC), 페이지네이션")
    void getMyReports_success_desc_paged() throws Exception {
        // given: 신고 2건 생성 (두 번째가 더 최신)
        UUID targetPid1 = testAdminProfileId;
        UUID targetPid2 = testAliceUserProfileId;

        var userToken = getToken(Role.ROLE_USER).accessToken();

        // 첫 번째 신고
        ExtractableResponse<Response> res1 = given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .contentType(ContentType.MULTIPART)
            .multiPart("targetProfileId", targetPid1.toString())
            .multiPart("category", "ABUSE")
            .multiPart("detail", "욕설")
            .when().post()
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .extract();

        UUID report1 = UUID.fromString(res1.jsonPath().getString("reportId"));
        reportsToBeDeleted.add(report1);

        // 살짝 간격을 두어 createdAt 순서를 확실히 함
        Thread.sleep(15);

        // 두 번째 신고
        ExtractableResponse<Response> res2 = given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .contentType(ContentType.MULTIPART)
            .multiPart("targetProfileId", targetPid2.toString())
            .multiPart("category", "SPAM")
            .multiPart("detail", "광고 도배")
            .when().post()
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .extract();

        UUID report2 = UUID.fromString(res2.jsonPath().getString("reportId"));
        reportsToBeDeleted.add(report2);

        // when: 목록 조회 (page=0, size=10)
        ExtractableResponse<Response> listRes = given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .contentType(ContentType.URLENC)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when().get()
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", greaterThanOrEqualTo(2))
            .extract();

        // then: 첫 번째 아이템이 "더 최신(두 번째 생성)"인지 확인
        String firstId = listRes.jsonPath().getString("content[0].reportId");
        String secondId = listRes.jsonPath().getString("content[1].reportId");

        Assertions.assertEquals(report2.toString(), firstId, "최신 생성건이 첫 번째여야 한다");
        Assertions.assertEquals(report1.toString(), secondId, "그 다음은 이전 생성건이어야 한다");

        // createdAt 존재/형식 간단 체크
        String createdAt0 = listRes.jsonPath().getString("content[0].createdAt");
        Assertions.assertNotNull(createdAt0);
    }

    @Test
    @DisplayName("내 신고 목록 조회 - 401 Unauthorized (토큰 없음)")
    void getMyReports_fail_401() {
        given().log().all()
            .contentType(ContentType.URLENC.withCharset(StandardCharsets.UTF_8))
            .queryParam("page", 0)
            .queryParam("size", 5)
            .when().get()
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("내 신고 목록 조회 - 페이지 사이즈 반영")
    void getMyReports_pagination_size() {
        // size=1로 요청하면 content가 최대 1개
        given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.URLENC)
            .queryParam("page", 0)
            .queryParam("size", 1)
            .when().get()
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", lessThanOrEqualTo(1));
    }
}
