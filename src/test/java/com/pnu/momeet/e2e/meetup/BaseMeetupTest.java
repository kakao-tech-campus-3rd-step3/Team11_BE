package com.pnu.momeet.e2e.meetup;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.repository.BlockRepository;
import com.pnu.momeet.domain.block.service.BlockDomainService;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberDomainService;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("meetup")
public abstract class BaseMeetupTest extends BaseE2ETest {

    protected static final String ALICE_EMAIL = "alice@test.com";
    protected static final String CHRIS_EMAIL = "chris@test.com";

    protected static final DateTimeFormatter REQUEST_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    protected Map<Role, TokenResponse> testTokens;

    protected Map<String, MemberResponse> users;
    protected Map<String, Profile> userProfiles;
    protected Map<String, TokenResponse> userTokens;

    protected List<UUID> toBeDeleted; // meetup IDs to delete

    @Autowired
    protected EmailAuthService emailAuthService;

    @Autowired
    protected MeetupDomainService meetupService;

    @Autowired
    protected MemberDomainService memberService;
    @Autowired
    protected ParticipantDomainService participantService;
    @Autowired
    protected BlockDomainService blockService;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private MeetupRepository meetupRepository;
    @Autowired
    private SigunguEntityService sigunguService;
    @Autowired
    private BlockRepository blockRepository;

    protected static LocalDateTime nextHalf(LocalDateTime t){
        t = t.withSecond(0).withNano(0);
        int m = t.getMinute(), add = (m % 30 == 0) ? 0 : (30 - (m % 30));
        return t.plusMinutes(add).withSecond(0).withNano(0);
    }
    protected static LocalDateTime baseSlot(){               // 최소 30분 여유
        return nextHalf(LocalDateTime.now()).plusMinutes(30);
    }
    protected static String slot(LocalDateTime base, int k){ // k 슬롯 뒤
        return base.plusMinutes(30L * k).format(REQUEST_FORMAT);
    }

    protected void blockIfNeeded(UUID blockerId, UUID blockedId) {
        try {
            blockService.createUserBlock(blockerId, blockedId);
        } catch (IllegalStateException e) {
            // 서비스에서 "이미 차단한 사용자입니다."로 던지는 경우 허용
            if (!e.getMessage().contains("이미 차단한 사용자")) throw e;
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // 경쟁 상태로 DataIntegrityViolationException → 409 변환된 경우 허용
            if (!(e.getStatusCode().value() == 409)) throw e;
        }
    }

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/meetups";
        testTokens = new EnumMap<>(Role.class);
        users = new HashMap<>();
        userProfiles = new HashMap<>();
        userTokens = new HashMap<>();
        toBeDeleted = new ArrayList<>();

        // 기본 토큰 준비
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));

        bootstrapSeedUser(ALICE_EMAIL);
        bootstrapSeedUser(CHRIS_EMAIL);
    }

    private void bootstrapSeedUser(String email) {
        // 1) 로그인 (비밀번호는 테스트 환경 공통 PW 사용)
        TokenResponse token = emailAuthService.login(email, TEST_USER_PASSWORD);
        Assertions.assertNotNull(token, "시드 사용자 로그인 실패: " + email);
        userTokens.put(email, token);

        // 2) 멤버 조회
        MemberResponse member = memberService.getMemberByEmail(email);
        Assertions.assertNotNull(member, "시드 멤버 없음: " + email);
        users.put(email, member);

        // 3) 프로필 조회
        Profile profile = profileRepository.findByMemberId(member.id())
            .orElseThrow(() -> new AssertionError("시드 프로필 없음: " + email));
        userProfiles.put(email, profile);
    }

    protected void createTestUser(String email) {
        // 1) 회원 생성
        var req = new MemberCreateRequest(email, TEST_USER_PASSWORD, List.of(Role.ROLE_USER.name()));
        MemberResponse member = memberService.saveMember(req);
        users.put(email, member);

        // 2) 로그인 토큰
        TokenResponse token = emailAuthService.login(email, TEST_USER_PASSWORD);
        userTokens.put(email, token);

        // 3) 프로필 생성 (닉네임 20자 제한)
        String baseNickname = email.split("@")[0];
        String nickname = baseNickname.length() > 20 ? baseNickname.substring(0, 20) : baseNickname;

        Sigungu sgg = sigunguService.getById(26410L); // 테스트 표준 지역
        Profile profile = profileRepository.save(Profile.create(
            member.id(),
            nickname,
            25,
            Gender.MALE,
            "https://example.com/profile.png",
            "테스트 유저입니다.",
            sgg
        ));
        userProfiles.put(email, profile);
    }

    @AfterEach
    protected void tearDown() {
        if (toBeDeleted != null && !toBeDeleted.isEmpty()) {
            for (UUID meetupId : toBeDeleted) {
                meetupRepository.deleteById(meetupId);
            }
            toBeDeleted.clear();
        }
    }

    protected TokenResponse getToken() {
        return getToken(Role.ROLE_USER);
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}