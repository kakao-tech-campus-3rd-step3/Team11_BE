package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberDomainService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.repository.SigunguRepository;
import com.pnu.momeet.domain.sigungu.service.SigunguDomainService;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("meetup")
public abstract class BaseMeetupTest extends BaseE2ETest {

    protected String ALICE_EMAIL;
    protected String BOB_EMAIL;

    protected Map<Role, TokenResponse> testTokens;

    protected Map<String, MemberResponse> users;
    protected Map<String, Profile> userProfiles;
    protected Map<String, TokenResponse> userTokens;

    protected List<UUID> memberToBeDeleted;
    protected List<UUID> profilesToBeDeleted;
    protected List<UUID> toBeDeleted; // meetup IDs to delete

    @Autowired
    protected EmailAuthService emailAuthService;

    @Autowired
    protected MeetupDomainService meetupService;

    @Autowired
    protected MemberDomainService memberService;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private MeetupRepository meetupRepository;
    @Autowired
    private SigunguEntityService sigunguService;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/meetups";
        testTokens = new EnumMap<>(Role.class);
        users = new HashMap<>();
        userProfiles = new HashMap<>();
        userTokens = new HashMap<>();
        memberToBeDeleted = new ArrayList<>();
        profilesToBeDeleted = new ArrayList<>();
        toBeDeleted = new ArrayList<>();

        // 기본 토큰 준비
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));

        // 매 테스트마다 고유한 기본 사용자 이메일 생성
        String uniqueSuffix = java.util.UUID.randomUUID().toString();
        ALICE_EMAIL = "alice-" + uniqueSuffix + "@test.com";
        BOB_EMAIL = "bob-" + uniqueSuffix + "@test.com";

        createTestUser(ALICE_EMAIL);
        createTestUser(BOB_EMAIL);
    }

    protected void createTestUser(String email) {
        var request= new MemberCreateRequest(
            email, TEST_USER_PASSWORD, List.of(Role.ROLE_USER.name())
        );
        users.put(email, memberService.saveMember(request));
        userTokens.put(email, emailAuthService.login(email, TEST_USER_PASSWORD));
        memberToBeDeleted.add(users.get(email).id());

        String baseNickname = email.split("@")[0];
        String nickname = baseNickname.length() > 20 ? baseNickname.substring(0, 20) : baseNickname;

        Sigungu sgg = sigunguService.getById(26410L);
        userProfiles.put(email, profileRepository.save(Profile.create(
                users.get(email).id(),
                nickname,
                25,
                Gender.MALE,
                "https://example.com/profile.png",
                "안녕하세요, 자기소개입니다.",
                sgg
        )));
        profilesToBeDeleted.add(userProfiles.get(email).getId());
    }

    @AfterEach
    protected void tearDown() {
        if (toBeDeleted != null && !toBeDeleted.isEmpty()) {
            for (UUID meetupId : toBeDeleted) {
                meetupRepository.deleteById(meetupId);
            }
            toBeDeleted.clear();
        }

        if (profilesToBeDeleted != null && !profilesToBeDeleted.isEmpty()) {
            for (UUID profileId : profilesToBeDeleted) {
                profileRepository.deleteById(profileId);
            }
            profilesToBeDeleted.clear();
        }

        if (memberToBeDeleted != null && !memberToBeDeleted.isEmpty()) {
            for (UUID memberId : memberToBeDeleted) {
                memberService.deleteMemberById(memberId);
            }
            memberToBeDeleted.clear();
        }
    }

    protected TokenResponse getToken() {
        return getToken(Role.ROLE_USER);
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}