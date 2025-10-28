package com.pnu.momeet.e2e.report;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import com.pnu.momeet.domain.report.repository.ReportAttachmentRepository;
import com.pnu.momeet.domain.report.repository.ReportRepository;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseReportTest extends BaseE2ETest {

    protected Map<Role, TokenResponse> testTokens;
    protected List<UUID> reportsToBeDeleted;
    protected List<UUID> attachmentToBeDeleted;

    protected UUID testUserMemberId;
    protected UUID testAdminMemberId;
    protected UUID testAliceUserMemberId;

    protected UUID testUserProfileId;
    protected UUID testAdminProfileId;
    protected UUID testAliceUserProfileId;

    @Autowired
    protected EmailAuthService emailAuthService;
    @Autowired
    protected MemberEntityService memberService;
    @Autowired
    protected ProfileDomainService profileService;
    @Autowired
    protected ReportRepository reportRepository;
    @Autowired
    protected ReportAttachmentRepository attachmentRepository;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/reports"; // 신고 컨트롤러 베이스 경로
        testTokens = new HashMap<>();
        attachmentToBeDeleted = new ArrayList<>();
        reportsToBeDeleted = new ArrayList<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER,  emailAuthService.login(TEST_USER_EMAIL,  TEST_USER_PASSWORD));

        // 테스트 계정(Admin, User, Alice)들의 memberId / profileId 확보
        var testAdminMember = memberService.getByEmail(TEST_ADMIN_EMAIL);
        testAdminMemberId = testAdminMember.getId();
        testAdminProfileId = profileService.getMyProfile(testAdminMemberId).id();
        var testUserMember = memberService.getByEmail(TEST_USER_EMAIL);
        testUserMemberId = testUserMember.getId();
        testUserProfileId = profileService.getMyProfile(testUserMemberId).id();
        var testAliceUserMember = memberService.getByEmail(TEST_ALICE_USER_EMAIL);
        testAliceUserMemberId = testAliceUserMember.getId();
        testAliceUserProfileId = profileService.getMyProfile(testAliceUserMemberId).id();
    }

    @AfterEach
    protected void tearDown() {
        if (reportsToBeDeleted != null && !reportsToBeDeleted.isEmpty()) {
            reportsToBeDeleted.forEach(badgeId -> reportRepository.deleteById(badgeId));
            reportsToBeDeleted.clear();
        }

        if (attachmentToBeDeleted != null && !attachmentToBeDeleted.isEmpty()) {
            attachmentToBeDeleted.forEach(badgeId -> attachmentRepository.deleteById(badgeId));
            attachmentToBeDeleted.clear();
        }
    }

    @AfterAll
    static void tearDownReport() {
        RestAssured.reset();
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}
