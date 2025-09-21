package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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

public class BadgeCreateTest extends BaseBadgeTest {

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3StorageService() {
            // 실제 S3 호출 방지 mock
            return Mockito.mock(S3StorageService.class);
        }
    }

    @Autowired
    private S3StorageService s3StorageService;

    private static final String ENDPOINT = "/badges";

    @Test
    @DisplayName("배지 생성 성공 - 201 Created + Location 헤더")
    void createBadge_success_201() throws Exception {
        // given
        String fakeIconUrl = "https://cdn.example.com/badges/uuid.png";
        Mockito.when(s3StorageService.uploadImage(any(), eq("/badges"))).thenReturn(fakeIconUrl);

        ClassPathResource img = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = img.getInputStream().readAllBytes();

        MultiPartSpecification namePart = new MultiPartSpecBuilder("모임 병아리")
            .controlName("name").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification descPart = new MultiPartSpecBuilder("첫 참여 배지")
            .controlName("description").charset(StandardCharsets.UTF_8).build();

        TokenResponse admin = getToken(Role.ROLE_ADMIN);

        // when
        var res = RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + admin.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(namePart)
            .multiPart(descPart)
            .multiPart("iconImage", "/image/badger.png", imageBytes, "image/png")
            .when()
            .post(ENDPOINT)
            .then().log().all()
            .statusCode(201)
            .header("Location", containsString("/api/profiles/badges/"))
            .body("badgeId", notNullValue())
            .body("name", containsString("모임 병아리"))
            .body("iconUrl", containsString("cdn.example.com/badges"))
            .extract();

        // then (사후 정리용으로 생성된 badgeId 저장)
        UUID createdBadgeId = UUID.fromString(res.jsonPath().getString("badgeId"));
        badgesToBeDeleted.add(createdBadgeId);
    }

    @Test
    @DisplayName("실패 - 401 Unauthorized (토큰 없음)")
    void createBadge_fail_401() throws Exception {
        ClassPathResource img = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = img.getInputStream().readAllBytes();

        RestAssured.given().log().all()
            .contentType(ContentType.MULTIPART)
            .multiPart("name", "테스트배지")
            .multiPart("description", "desc")
            .multiPart("iconImage", "/image/badger.png", imageBytes, "image/png")
            .when()
            .post(ENDPOINT)
            .then().log().all()
            .statusCode(401);
    }

    @Test
    @DisplayName("실패 - 403 Forbidden (USER 권한)")
    void createBadge_fail_403_userRole() throws Exception {
        ClassPathResource img = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = img.getInputStream().readAllBytes();

        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("name", "권한테스트")
            .multiPart("description", "USER 는 금지")
            .multiPart("iconImage", "/image/badger.png", imageBytes, "image/png")
            .when()
            .post(ENDPOINT)
            .then().log().all()
            .statusCode(403);
    }

    @Test
    @DisplayName("실패 - 400 Bad Request (유효성: 이름 짧음/아이콘 누락)")
    void createBadge_fail_400_validation() {
        // 이름 너무 짧게 + 아이콘 미첨부
        RestAssured.given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("name", "뱃") // min=2 위반
            .multiPart("description", "설명")
            .when()
            .post(ENDPOINT)
            .then().log().all()
            .statusCode(400);
    }

    @Test
    @DisplayName("실패 - 409 Conflict (이름 중복)")
    void createBadge_fail_409_duplicateName() throws Exception {
        // given: 먼저 하나 만들고 같은 이름으로 다시 요청
        String fakeIconUrl = "https://cdn.example.com/badges/dup.png";
        Mockito.when(s3StorageService.uploadImage(any(), eq("/badges"))).thenReturn(fakeIconUrl);

        ClassPathResource img = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = img.getInputStream().readAllBytes();

        var first = RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("name", "중복테스트")
            .multiPart("description", "1회차")
            .multiPart("iconImage", "/image/badger.png", imageBytes, "image/png")
            .when().post(ENDPOINT)
            .then().statusCode(201).extract();

        UUID created = UUID.fromString(first.jsonPath().getString("badgeId"));
        badgesToBeDeleted.add(created);

        // when & then: 같은 이름 재요청 → 400
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("name", "중복테스트")
            .multiPart("description", "2회차")
            .multiPart("iconImage", "/image/badger.png", imageBytes, "image/png")
            .when()
            .post(ENDPOINT)
            .then().log().all()
            .statusCode(400);
    }
}