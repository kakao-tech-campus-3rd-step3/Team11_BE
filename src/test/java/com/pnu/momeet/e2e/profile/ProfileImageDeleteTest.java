package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

@Import(ProfileImageDeleteTest.MockS3Config.class)
class ProfileImageDeleteTest extends BaseProfileTest {

    @Autowired
    private S3StorageService s3StorageService;

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3UploaderService() {
            // 실제 S3 호출 방지용 mock 빈
            return Mockito.mock(S3StorageService.class);
        }
    }

    @Test
    @DisplayName("[E2E] 프로필 이미지 삭제: 204, 이후 조회시 imageUrl=null, 두 번 호출해도 204(멱등)")
    void deleteProfileImage_flow() {
        // 시드 USER 토큰 (data.sql로 미리 존재)
        TokenPair token = getToken(Role.ROLE_USER);

        // 업로더 mock: 정상 URL 반환
        Mockito.reset(s3StorageService);
        BDDMockito.given(s3StorageService.uploadImage(any(), eq("profiles/")))
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

        // 2) 삭제 호출 → 204
        BDDMockito.willDoNothing().given(s3StorageService).deleteImage(any());
        RestAssured
            .given().log().all()
                .header("Authorization", "Bearer " + token.accessToken())
            .when()
                .delete("/image")
            .then().log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // 3) 프로필 조회 → imageUrl == null
        RestAssured
            .given().log().all()
                .header("Authorization", "Bearer " + token.accessToken())
            .when()
                .get("/me")
            .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("imageUrl", Matchers.nullValue());

        // 4) 다시 삭제 호출(멱등) → 204
        RestAssured
            .given().log().all()
                .header("Authorization", "Bearer " + token.accessToken())
            .when()
                .delete("/image")
            .then().log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("[E2E] 프로필 이미지 삭제: 미인증이면 401")
    void deleteProfileImage_unauthorized() {
        RestAssured
            .given().log().all()
            .when()
                .delete("/image")
            .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
