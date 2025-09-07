package com.pnu.momeet.e2e.member;

import com.pnu.momeet.common.model.TokenPair;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberService;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Tag("member")
public abstract class BaseMemberTest extends BaseE2ETest {

    protected Map<Role, TokenPair> testTokens;
    protected List<UUID> toBeDeleted;

    @Autowired
    protected EmailAuthService emailAuthService;

    @Autowired
    protected MemberService memberService;


    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/members";
        testTokens = new HashMap<>();
        toBeDeleted = new ArrayList<>();
        testTokens.put(Role.ROLE_ADMIN, emailAuthService.login(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD));
        testTokens.put(Role.ROLE_USER, emailAuthService.login(TEST_USER_EMAIL, TEST_USER_PASSWORD));
    }

    @AfterEach
    protected void tearDown() {
        if (toBeDeleted != null && !toBeDeleted.isEmpty()) {
            toBeDeleted.forEach(memberId -> memberService.deleteMemberById(memberId));
            toBeDeleted.clear();
        }
    }

    protected TokenPair getToken() {
        return getToken(Role.ROLE_ADMIN);
    }

    protected TokenPair getToken(Role role) {
        return testTokens.get(role);
    }
}
