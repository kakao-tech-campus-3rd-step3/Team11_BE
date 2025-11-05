package com.pnu.momeet.e2e.profile;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.member.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class ProfileDeleteTest extends BaseProfileTest {

    @MockitoBean
    S3StorageService s3StorageService; // AFTER_COMMIT 호출만 확인

    @Test
    @DisplayName("내 프로필 삭제: 204 -> 이후 조회 404, S3 삭제 트리거 호출")
    void deleteMyProfile_then204_andThen404_andS3CleanupTriggered() {
        String token = getToken(Role.ROLE_USER).accessToken();

        // delete
        given()
            .log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + token)
            .when()
            .delete("/me")
            .then()
            .statusCode(204);

        // 재조회 404
        given()
            .log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + token)
            .when()
            .get("/me")
            .then()
            .statusCode(404);

        // AFTER_COMMIT 비동기 삭제 트리거
        verify(s3StorageService, atLeastOnce()).deleteImage(anyString());
    }
}
