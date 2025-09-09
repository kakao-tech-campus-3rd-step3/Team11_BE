package com.pnu.momeet.e2e.sigungu;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Tag("sigungu")
public abstract class BaseSigunguTest extends BaseE2ETest {

    protected Map<Role, TokenResponse> testTokens;

    @Autowired
    protected EmailAuthService emailAuthService;

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/sigungu";
        testTokens = new HashMap<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));
    }

    protected TokenResponse getToken() {
        return getToken(Role.ROLE_ADMIN);
    }

    protected TokenResponse getToken(Role role) {
        return testTokens.get(role);
    }
}
