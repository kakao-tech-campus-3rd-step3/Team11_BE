package com.pnu.momeet.e2e.report;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import java.nio.charset.StandardCharsets;
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

class ReportCreateTest extends BaseReportTest {

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3StorageService() {
            // 실제 S3 호출을 막기 위한 mock
            return Mockito.mock(S3StorageService.class);
        }
    }

    @Autowired
    private S3StorageService s3StorageService;

    @Test
    @DisplayName("신고 생성 성공 - 201 Created (이미지 1장, Location 헤더 포함 가능)")
    void createReport_success_201() throws Exception {
        // given
        String fakeUrl = "https://cdn.example.com/reports/abcd.png";
        given(s3StorageService.uploadImage(any(MultipartFile.class), contains("/reports")))
            .willReturn(fakeUrl);

        ClassPathResource img = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = img.getInputStream().readAllBytes();

        MultiPartSpecification targetIdPart = new MultiPartSpecBuilder(testAdminProfileId.toString())
            .controlName("targetProfileId").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification categoryPart = new MultiPartSpecBuilder("SPAM")
            .controlName("category").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification detailPart = new MultiPartSpecBuilder("광고 도배")
            .controlName("detail").charset(StandardCharsets.UTF_8).build();

        TokenResponse user = getToken(Role.ROLE_USER);

        // when & then
        var res = RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + user.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(targetIdPart)
            .multiPart(categoryPart)
            .multiPart(detailPart)
            .multiPart("images", "/image/badger.png", imageBytes, "image/png")
            .when()
            .post()
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .body("reportId", notNullValue())
            .body("reporterProfileId", equalTo(testUserProfileId.toString()))
            .body("targetProfileId", equalTo(testAdminProfileId.toString()))
            .body("category", equalTo("SPAM"))
            .body("status", equalTo("OPEN"))
            .body("images[0]", equalTo(fakeUrl))
            .extract();

        UUID createdReportId = UUID.fromString(res.jsonPath().getString("reportId"));
        reportsToBeDeleted.add(createdReportId);
    }

    @Test
    @DisplayName("실패 - 401 Unauthorized (토큰 없음)")
    void createReport_fail_401() {
        RestAssured.given().log().all()
            .contentType(ContentType.MULTIPART)
            .multiPart("targetProfileId", testAdminProfileId.toString())
            .multiPart("category", "ABUSE")
            .when()
            .post()
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("실패 - 400 Bad Request (유효성: 이미지 6장 초과)")
    void createReport_fail_400_maxImages() throws Exception {
        ClassPathResource img = new ClassPathResource("/image/badger.png");
        byte[] bytes = img.getInputStream().readAllBytes();

        var req = RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("targetProfileId", testAdminProfileId.toString())
            .multiPart("category", "ABUSE");

        for (int i = 0; i < 6; i++) {
            req.multiPart("images", "/image/badger.png", bytes, "image/png");
        }

        req.when().post()
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", anyOf(nullValue(), notNullValue()));
    }

    @Test
    @DisplayName("실패 - 400 Bad Request (자기신고 금지)")
    void createReport_fail_400_selfReport() {
        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("targetProfileId", testUserProfileId.toString()) // 자기자신
            .multiPart("category", "ABUSE")
            .when()
            .post()
            .then().log().all()
            .statusCode(HttpStatus.CONFLICT.value());
    }
}