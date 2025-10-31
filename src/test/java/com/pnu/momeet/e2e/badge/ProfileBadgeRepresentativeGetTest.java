package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeRepository;
import com.pnu.momeet.domain.badge.service.ProfileBadgeEntityService;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProfileBadgeRepresentativeGetTest extends BaseProfileBadgeTest {

    @Autowired
    BadgeRepository badgeRepository;

    @Autowired
    ProfileBadgeRepository profileBadgeRepository;

    @Autowired
    ProfileBadgeEntityService entityService;

    private UUID createdBadgeId;

    @AfterEach
    void cleanup() {
        // 테스트 생성 데이터 정리 + 대표 배지 리셋
        if (createdBadgeId != null) {
            badgeRepository.deleteById(createdBadgeId);
            createdBadgeId = null;
        }
        entityService.resetRepresentative(testUserProfileId);
    }

    @Test
    @DisplayName("특정 프로필 대표 배지 조회 - 대표 배지 존재 → 200 OK")
    void getUserRepresentativeBadge_ok() {
        // given: 배지 생성 + 매핑 + 대표 설정
        var badge = Badge.create(
            "[TEST] 대표 배지",
            "E2E 전용",
            "https://cdn.example.com/badges/test.png",
            "REP_TEST"
        );
        createdBadgeId = badgeRepository.save(badge).getId();

        profileBadgeRepository.save(new ProfileBadge(testUserProfileId, createdBadgeId));
        entityService.resetRepresentative(testUserProfileId);
        entityService.setRepresentative(testUserProfileId, createdBadgeId);

        String accessToken = getToken(Role.ROLE_USER).accessToken();

        // when & then
        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}/badges/representative", testUserProfileId)
            .then()
            .log().all()
            .statusCode(200)
            .body("badgeId", notNullValue())
            .body("name", equalTo("[TEST] 대표 배지"))
            .body("description", equalTo("E2E 전용"))
            .body("iconUrl", equalTo("https://cdn.example.com/badges/test.png"))
            .body("code", equalTo("REP_TEST"))
            .body("representative", equalTo(true));
    }

    @Test
    @DisplayName("특정 프로필 대표 배지 조회 - 대표 배지 없음 → 204 No Content")
    void getUserRepresentativeBadge_noContent() {
        // given
        entityService.resetRepresentative(testUserProfileId);
        String accessToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}/badges/representative", testUserProfileId)
            .then()
            .log().all()
            .statusCode(204); // 바디 검증 금지
    }

    @Test
    @DisplayName("특정 프로필 대표 배지 조회 - 프로필 없음 → 404")
    void getUserRepresentativeBadge_notFound() {
        String accessToken = getToken(Role.ROLE_USER).accessToken();
        UUID unknown = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-000000000000");

        RestAssured.given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}/badges/representative", unknown)
            .then()
            .log().all()
            .statusCode(404);
    }

    @Test
    @DisplayName("특정 프로필 대표 배지 조회 - 인증 누락 → 401")
    void getUserRepresentativeBadge_unauthorized() {
        RestAssured.given()
            .when()
            .get("/{profileId}/badges/representative", testUserProfileId)
            .then()
            .log().all()
            .statusCode(401);
    }
}
