package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

@Tag("e2e")
class BadgeUpdateTest extends BaseBadgeTest {

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private S3StorageService s3StorageService;

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3StorageService() {
            return Mockito.mock(S3StorageService.class);
        }
    }

    private UUID targetBadgeId;
    private final String initialIconUrl = "https://cdn.example.com/badges/old.png";

    @BeforeEach
    void prepareBadge() {
        // 테스트용 배지 한 개 생성 (아이콘 URL 포함)
        Badge badge = Badge.create(
            "[E2E] 수정대상",
            "원래 설명",
            initialIconUrl
        );
        badgeRepository.save(badge);

        targetBadgeId = badge.getId();
        badgesToBeDeleted.add(targetBadgeId);
    }

    @Test
    @DisplayName("배지 수정 성공 - (이름/설명 변경 + 새 아이콘 교체) 200 OK")
    void updateBadge_success_withNewIcon() throws Exception {
        // given
        String newUrl = "https://cdn.example.com/badges/new-uuid.png";
        willDoNothing().given(s3StorageService).deleteImage(initialIconUrl);
        given(s3StorageService.uploadImage(any(), anyString())).willReturn(newUrl);

        // 멀티파트 구성 (한글 인코딩 적용)
        MultiPartSpecification namePart = new MultiPartSpecBuilder("수정된배지명")
            .controlName("name").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification descPart = new MultiPartSpecBuilder("수정된 설명")
            .controlName("description").charset(StandardCharsets.UTF_8).build();

        byte[] imageBytes = new ClassPathResource("/image/badger.png").getInputStream().readAllBytes();

        TokenResponse admin = getToken(Role.ROLE_ADMIN);

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + admin.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(namePart)
            .multiPart(descPart)
            .multiPart("iconImage", "badge.png", imageBytes, "image/png")
            .when()
            .patch("/badges/{badgeId}", targetBadgeId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("badgeId", notNullValue())
            .body("name", equalTo("수정된배지명"))
            .body("description", equalTo("수정된 설명"))
            .body("iconUrl", equalTo(newUrl));

        // verify S3
        verify(s3StorageService, times(1)).uploadImage(any(), anyString());
        verify(s3StorageService, times(1)).deleteImage(eq(initialIconUrl));
    }

    @Test
    @DisplayName("배지 수정 성공 - (텍스트만 변경, 아이콘 미전송) 200 OK")
    void updateBadge_success_withoutIcon() {
        // given
        Mockito.reset(s3StorageService);

        MultiPartSpecification namePart = new MultiPartSpecBuilder("텍스트만수정")
            .controlName("name").charset(StandardCharsets.UTF_8).build();

        TokenResponse admin = getToken(Role.ROLE_ADMIN);

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + admin.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(namePart)
            .when()
            .patch("/badges/{badgeId}", targetBadgeId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("name", equalTo("텍스트만수정"))
            .body("iconUrl", equalTo(initialIconUrl)); // 기존 아이콘 유지

        verify(s3StorageService, never()).uploadImage(any(), anyString());
        verify(s3StorageService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("배지 수정 실패 - 401 Unauthorized (토큰 없음)")
    void updateBadge_fail_unauthorized() {
        MultiPartSpecification namePart = new MultiPartSpecBuilder("수정시도")
            .controlName("name").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .contentType(ContentType.MULTIPART)
            .multiPart(namePart)
            .when()
            .patch("/badges/{badgeId}", targetBadgeId)
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("배지 수정 실패 - 403 Forbidden (USER 권한)")
    void updateBadge_fail_forbidden_userRole() {
        MultiPartSpecification namePart = new MultiPartSpecBuilder("수정시도")
            .controlName("name").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(namePart)
            .when()
            .patch("/badges/{badgeId}", targetBadgeId)
            .then().log().all()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("배지 수정 실패 - 404 Not Found (대상 배지 없음)")
    void updateBadge_fail_notFound() {
        UUID notExist = UUID.randomUUID();
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(new MultiPartSpecBuilder("수정").controlName("name").build())
            .when()
            .patch("/badges/{badgeId}", notExist)
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("배지 수정 실패 - 400 Bad Request (이름 길이 2 미만)")
    void updateBadge_fail_validation_nameTooShort() {
        MultiPartSpecification badName = new MultiPartSpecBuilder("ㅋ")
            .controlName("name").charset(StandardCharsets.UTF_8).build();
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(badName)
            .when()
            .patch("/badges/{badgeId}", targetBadgeId)
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
