package com.pnu.momeet.e2e.evaluation;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BaseMeetupEvaluationTest extends BaseE2ETest {

    protected Map<Role, TokenResponse> testTokens;
    protected List<UUID> evaluationsToBeDeleted;
    protected UUID evaluator_profile_uuid;       // 테스트유저
    protected UUID target_admin_profile_uuid;    // 관리자
    protected UUID test_meetup_id;               // 종료된 테스트 모임

    @Autowired
    protected EmailAuthService emailAuthService;

    @Autowired
    protected MemberDomainService memberService;

    @Autowired
    protected ProfileDomainService profileService;

    @Autowired
    protected MeetupDomainService meetupService;

    @Autowired
    protected EvaluationRepository evaluationRepository;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/meetups";
        testTokens = new HashMap<>();
        evaluationsToBeDeleted = new ArrayList<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));

        // 테스트용 프로필 ID 설정
        // evaluator = user@test.com
        var user = memberService.getMemberByEmail(TEST_USER_EMAIL);
        evaluator_profile_uuid = profileService.getMyProfile(user.id()).id();

        // target = admin@test.com (동일 모임 참가자)
        var admin = memberService.getMemberByEmail(TEST_ADMIN_EMAIL);
        target_admin_profile_uuid = profileService.getMyProfile(admin.id()).id();

        // 종료된 모임 1건
        Page<Meetup> ended = meetupService.findEndedMeetupsByProfileId(
            evaluator_profile_uuid, PageRequest.of(0, 1)
        );
        if (ended.isEmpty()) {
            throw new IllegalStateException("종료된 모임이 필요합니다. test_init_data.sql 확인 필요");
        }
        test_meetup_id = ended.getContent().getFirst().getId();
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
