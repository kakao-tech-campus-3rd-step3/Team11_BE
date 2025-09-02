package com.pnu.momeet.e2e.profile;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("profile")
public abstract class BaseProfileTest extends BaseE2ETest {

    protected Map<Role, TokenPair> testTokens;

    @Autowired
    protected EmailAuthService emailAuthService;

    public static final String TEST_USER_PROFILE_NICKNAME = "테스트유저";
    public static final String TEST_USER_PROFILE_LOCATION = "부산 금정구";
    public static final int TEST_USER_PROFILE_AGE = 25;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/profiles";
        testTokens = new HashMap<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));
    }

    protected TokenPair getToken() {
        return getToken(Role.ROLE_USER);
    }

    protected TokenPair getToken(Role role) {
        return testTokens.get(role);
    }
}
