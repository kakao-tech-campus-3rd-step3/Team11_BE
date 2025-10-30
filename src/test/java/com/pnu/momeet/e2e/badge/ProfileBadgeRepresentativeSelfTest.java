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
import org.springframework.transaction.annotation.Transactional;

public class ProfileBadgeRepresentativeSelfTest extends BaseProfileBadgeTest {

    @Autowired
    BadgeRepository badgeRepository;

    @Autowired
    ProfileBadgeRepository profileBadgeRepository;

    @Autowired
    ProfileBadgeEntityService entityService;

    private UUID createdBadgeId;

    @Transactional
    @AfterEach
    void cleanUp() {
        if (createdBadgeId != null) {
            badgeRepository.deleteById(createdBadgeId);
            createdBadgeId = null;
        }
        // 대표 배지 리셋
        entityService.resetRepresentative(testUserProfileId);
    }

    @Test
    @DisplayName("내 대표 배지 조회 - 대표 배지 존재 시 200 OK")
    void getMyRepresentativeBadge_ok() {
        // given - 테스트 유저에게 배지 부여 + 대표 설정
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
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/me/badges/representative")
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
    @DisplayName("내 대표 배지 조회 - 대표 배지 없으면 204 No Content")
    void getMyRepresentativeBadge_noContent() {
        // given - 대표 배지 리셋
        entityService.resetRepresentative(testUserProfileId);

        String accessToken = getToken(Role.ROLE_USER).accessToken();

        // when & then
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/me/badges/representative")
            .then()
            .log().all()
            .statusCode(204);
    }

    @Test
    @DisplayName("내 대표 배지 조회 - 인증 없음 → 401")
    void getMyRepresentativeBadge_unauthorized() {
        RestAssured
            .given()
            .when()
            .get("/me/badges/representative")
            .then()
            .log().all()
            .statusCode(401);
    }
}
