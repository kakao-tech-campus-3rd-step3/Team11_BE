// src/test/java/com/pnu/momeet/e2e/profile/ProfileImageUploadTest.java
package com.pnu.momeet.e2e.profile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

@Import(ProfileImageUploadTest.MockS3Config.class)
class ProfileImageUploadTest extends BaseProfileTest {

    @Autowired
    private S3StorageService s3StorageService;

    @TestConfiguration
    static class MockS3Config {
        @Bean @Primary
        S3StorageService s3UploaderService() {
            // 실제 S3 호출 방지용 mock 빈
            return Mockito.mock(S3StorageService.class);
        }
    }

    @Test
    @DisplayName("프로필 이미지 업로드 성공 (200)")
    void upload_success() {
        // 시드 USER 토큰 (data.sql로 미리 존재)
        TokenPair token = getToken(Role.ROLE_USER);

        // 업로더 mock: 정상 URL 반환
        Mockito.reset(s3StorageService);
        given(s3StorageService.uploadImage(any(), eq("profiles/")))
            .willReturn("https://cdn.example.com/profiles/uuid.png");
        RestAssured
            .given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "avatar.jpg", new byte[]{1,2,3}, "image/jpeg")
            .when()
                .post("/image")
            .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("imageUrl", containsString("profiles/"));
    }

    @Test
    @DisplayName("인증 실패 (401) - 토큰 없음")
    void upload_unauthorized() {
        RestAssured
            .given().log().all()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "avatar.jpg", new byte[]{1,2,3}, "image/jpeg")
            .when()
                .post("/image")
            .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("프로필 없음 (404) - 관리자 시드 계정")
    void upload_profile_not_found() {
        TokenPair admin = getToken(Role.ROLE_ADMIN);

        RestAssured
            .given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + admin.accessToken())
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "avatar.jpg", new byte[]{1,2,3}, "image/jpeg")
            .when()
                .post("/image")
            .then().log().all()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("검증 실패 (400) - 허용되지 않은 확장자")
    void upload_bad_extension() {
        TokenPair token = getToken(Role.ROLE_USER);

        Mockito.reset(s3StorageService);

        given(s3StorageService.uploadImage(any(), eq("profiles/")))
            .willThrow(new IllegalArgumentException("허용되지 않은 확장자입니다."));

        RestAssured
            .given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "evil.exe", new byte[]{1,2,3}, "application/octet-stream")
            .when()
                .post("/image")
            .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", containsString("허용되지 않은 확장자입니다."));
    }

    @Test
    @DisplayName("검증 실패 (400) - 파일 크기 5MB 초과")
    void upload_exceed_size() {
        TokenPair token = getToken(Role.ROLE_USER);

        Mockito.reset(s3StorageService);
        given(s3StorageService.uploadImage(any(), eq("profiles/")))
            .willThrow(new IllegalArgumentException("파일 크기가 5MB를 초과했습니다."));

        RestAssured
            .given().log().all()
                .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
                .contentType(ContentType.MULTIPART)
                // 실제 6MB 바이트 전송 대신 업로더 mock이 예외를 던지도록 시뮬레이션
                .multiPart("image", "big.png", new byte[]{1,2,3}, "image/png")
            .when()
                .post("/image")
            .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", containsString("파일 크기가 5MB를 초과했습니다."));
    }
}