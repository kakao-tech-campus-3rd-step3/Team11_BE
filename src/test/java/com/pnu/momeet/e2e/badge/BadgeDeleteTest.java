package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

class BadgeDeleteTest extends BaseBadgeTest {

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private S3StorageService s3StorageService;

    private UUID badgeIdToDelete;
    private String oldIconUrl;

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3StorageService() {
            return Mockito.mock(S3StorageService.class);
        }
    }

    @BeforeEach
    void prepareBadge() {
        // 배지 생성
        Badge badge = Badge.create(
            "삭제용-배지",
            "설명",
            "https://cdn.example.com/badges/d.png",
            "DELETE"
        );
        badge = badgeRepository.save(badge);
        badgeIdToDelete = badge.getId();
        oldIconUrl = badge.getIconUrl();
        badgesToBeDeleted.add(badgeIdToDelete);
    }

    @Test
    @DisplayName("[ADMIN] 배지 삭제 성공 → 204 No Content")
    void delete_success_admin() {
        TokenResponse admin = getToken(Role.ROLE_ADMIN);

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + admin.accessToken())
            .when()
            .delete("/badges/{badgeId}", badgeIdToDelete)
            .then()
            .statusCode(204);

        verify(s3StorageService, times(1)).deleteImage(eq(oldIconUrl));
    }

    @Test
    @DisplayName("[USER] 권한 없음 → 403 Forbidden")
    void delete_forbidden_user() {
        TokenResponse user = getToken(Role.ROLE_USER);

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + user.accessToken())
            .when()
            .delete("/badges/{badgeId}", badgeIdToDelete)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("[ADMIN] 대상 없음 → 404 Not Found(+메시지)")
    void delete_notFound_admin() {
        TokenResponse admin = getToken(Role.ROLE_ADMIN);
        UUID random = UUID.randomUUID();

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + admin.accessToken())
            .when()
            .delete("/badges/{badgeId}", random)
            .then()
            .statusCode(404)
            .body("detail", equalTo("존재하지 않는 배지입니다."));;
    }
}