package com.pnu.momeet.e2e.report;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import com.pnu.momeet.domain.report.repository.ReportRepository;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

class ReportDeleteTest extends BaseReportTest {

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3StorageService() { return Mockito.mock(S3StorageService.class); }
    }

    @Autowired
    private S3StorageService s3StorageService;
    @Autowired private ReportRepository reportRepository;

    private UUID createReportAsUser() throws Exception {
        // 이미지 1장 업로드가 포함된 신고 생성
        String fakeUrl = "https://cdn.example.com/reports/for-delete.png";
        given(s3StorageService.uploadImage(any(MultipartFile.class), contains("/reports")))
            .willReturn(fakeUrl);

        var img = new ClassPathResource("/image/badger.png").getInputStream().readAllBytes();

        TokenResponse user = getToken(Role.ROLE_USER);

        MultiPartSpecification targetIdPart = new MultiPartSpecBuilder(testAdminProfileId.toString())
            .controlName("targetProfileId")
            .mimeType("text/plain; charset=UTF-8")
            .charset(StandardCharsets.UTF_8)
            .build();

        MultiPartSpecification categoryPart = new MultiPartSpecBuilder("SPAM")
            .controlName("category")
            .mimeType("text/plain; charset=UTF-8")
            .charset(StandardCharsets.UTF_8)
            .build();

        MultiPartSpecification detailPart = new MultiPartSpecBuilder("삭제 테스트")
            .controlName("detail")
            .mimeType("text/plain; charset=UTF-8")
            .charset(StandardCharsets.UTF_8)
            .build();

        var res = RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + user.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(targetIdPart)
            .multiPart(categoryPart)
            .multiPart(detailPart)
            .multiPart("images", "/image/badger.png", img, "image/png")
            .when().post()
            .then().statusCode(201)
            .extract().response();

        UUID reportId = UUID.fromString(res.jsonPath().getString("reportId"));
        reportsToBeDeleted.add(reportId);
        return reportId;
    }

    @Test
    @DisplayName("삭제 성공 - 작성자 본인이 OPEN 신고 삭제 시 204 No Content")
    void deleteReport_success_byOwner_open() throws Exception {
        UUID reportId = createReportAsUser();

        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{reportId}", reportId)
            .then().log().all()
            .statusCode(HttpStatus.NO_CONTENT.value());

        reportsToBeDeleted.remove(reportId);
    }

    @Test
    @DisplayName("삭제 실패 - 제3자(관리자 등) 삭제 시 권한/정책 위반")
    void deleteReport_fail_forbidden_ifNotOwner() throws Exception {
        UUID reportId = createReportAsUser();

        // 관리자 토큰으로 사용자 신고를 삭제 시도
        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .when()
            .delete("/{reportId}", reportId)
            .then().log().all()
            .statusCode(anyOf(is(403), is(409), is(400)));
    }

    @Test
    @DisplayName("삭제 실패 - ENDED 상태 신고는 409 Conflict")
    void deleteReport_fail_conflict_ifEnded() throws Exception {
        UUID reportId = createReportAsUser();

        // DB에 직접 접근하여 상태를 ENDED로 변경
        UserReport report = reportRepository.findById(reportId).orElseThrow();
        report.processReport(testAdminProfileId, "삭제 테스트");
        reportRepository.save(report);

        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{reportId}", reportId)
            .then().log().all()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("삭제 실패 - 존재하지 않는 신고는 404 Not Found")
    void deleteReport_fail_notFound() {
        UUID notExists = UUID.randomUUID();

        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
            .delete("/{reportId}", notExists)
            .then().log().all()
            .statusCode(anyOf(is(404), is(400)));
    }
}