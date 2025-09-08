package com.pnu.momeet.e2e.profile;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberService;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.profile.service.ProfileService;
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

@Tag("profile")
public abstract class BaseProfileTest extends BaseE2ETest {

    protected Map<Role, TokenResponse> testTokens;
    protected List<UUID> membersToBeDeleted;
    protected List<UUID> profilesToBeDeleted;
    protected UUID test_user_profile_uuid;

    @Autowired
    protected EmailAuthService emailAuthService;

    @Autowired
    protected MemberService memberService;

    @Autowired
    protected ProfileService profileService;

    @Autowired
    protected ProfileRepository profileRepository;

    public static final String TEST_USER_PROFILE_NICKNAME = "테스트유저";
    public static final String TEST_USER_PROFILE_LOCATION = "부산 금정구";
    public static final int TEST_USER_PROFILE_AGE = 25;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/profiles";
        testTokens = new HashMap<>();
        membersToBeDeleted = new ArrayList<>();
        profilesToBeDeleted = new ArrayList<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));

        // 테스트용 프로필 ID 설정
        var testMember = memberService.findMemberByEmail(TEST_USER_EMAIL);
        test_user_profile_uuid = profileService.getMyProfile(testMember.id()).id();
    }

    @AfterEach
    protected void tearDown() {
        if (profilesToBeDeleted != null && !profilesToBeDeleted.isEmpty()) {
            profilesToBeDeleted.forEach(profileId -> profileRepository.deleteById(profileId));
            profilesToBeDeleted.clear();
        }

        if (membersToBeDeleted != null && !membersToBeDeleted.isEmpty()) {
            membersToBeDeleted.forEach(memberId -> memberService.deleteMemberById(memberId));
            membersToBeDeleted.clear();
        }
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}