package com.pnu.momeet.e2e.badge;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberDomainService;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("badge")
public abstract class BaseBadgeTest extends BaseE2ETest {

    protected Map<Role, TokenResponse> testTokens;
    protected List<UUID> badgesToBeDeleted;
    protected UUID testUserProfileId;

    @Autowired
    protected EmailAuthService emailAuthService;
    @Autowired
    protected MemberDomainService memberService;
    @Autowired
    protected ProfileDomainService profileService;
    @Autowired
    protected BadgeRepository badgeRepository;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/profiles"; // 배지 컨트롤러 베이스 경로
        testTokens = new HashMap<>();
        badgesToBeDeleted = new ArrayList<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER,  emailAuthService.login(TEST_USER_EMAIL,  TEST_USER_PASSWORD));

        // 테스트 유저의 프로필 ID 확보 (E2E 시나리오 용)
        var testMember = memberService.getMemberByEmail(TEST_USER_EMAIL);
        testUserProfileId = profileService.getMyProfile(testMember.id()).id();
    }

    @AfterEach
    protected void tearDown() {
        if (badgesToBeDeleted != null && !badgesToBeDeleted.isEmpty()) {
            badgesToBeDeleted.forEach(badgeId -> badgeRepository.deleteById(badgeId));
            badgesToBeDeleted.clear();
        }
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}