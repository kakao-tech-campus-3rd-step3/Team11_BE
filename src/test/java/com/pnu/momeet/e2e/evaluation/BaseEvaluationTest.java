package com.pnu.momeet.e2e.evaluation;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberService;
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

@Tag("evaluation")
public abstract class BaseEvaluationTest extends BaseE2ETest {

    protected Map<Role, TokenResponse> testTokens;
    protected List<UUID> evaluationsToBeDeleted;
    protected UUID test_user_profile_uuid;

    @Autowired
    protected EmailAuthService emailAuthService;

    @Autowired
    protected MemberService memberService;

    @Autowired
    protected ProfileService profileService;

    @Autowired
    protected EvaluationRepository evaluationRepository;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/evaluations";
        testTokens = new HashMap<>();
        evaluationsToBeDeleted = new ArrayList<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));

        // 테스트용 프로필 ID 설정
        var testMember = memberService.findMemberByEmail(TEST_USER_EMAIL);
        test_user_profile_uuid = profileService.getMyProfile(testMember.id()).id();
    }

    @AfterEach
    protected void tearDown() {
        if (evaluationsToBeDeleted != null && !evaluationsToBeDeleted.isEmpty()) {
            evaluationsToBeDeleted.forEach(id -> evaluationRepository.deleteById(id));
            evaluationsToBeDeleted.clear();
        }
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}
