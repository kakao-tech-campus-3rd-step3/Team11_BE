package com.pnu.momeet.e2e.auth;

import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberService;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("auth")
public abstract class BaseAuthTest extends BaseE2ETest {

    @Autowired
    protected MemberService memberService;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    protected MemberResponse testMember;
    protected List<MemberResponse> toBeDeleted;

    protected String testPassword = "testAuth123@";

    @BeforeEach
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/auth";
        toBeDeleted = new ArrayList<>();
        testMember = memberService.saveMember(new Member("testAuth@test.com", testPassword, List.of(Role.ROLE_USER)));
        toBeDeleted.add(testMember);
    }

    @AfterEach
    protected void tearDown() {
        if (toBeDeleted != null && !toBeDeleted.isEmpty()) {
            toBeDeleted.forEach(member -> memberService.deleteMemberById(member.id()));
            toBeDeleted.clear();
        }
    }

    protected void testTokenPair(TokenResponse response, MemberResponse member) {
        TokenInfo accessTokenInfo = jwtTokenProvider.parseToken(response.accessToken());
        assertThat(accessTokenInfo.subject()).isEqualTo(member.id().toString());
        assertThat(accessTokenInfo.expiresAt()).isAfter(LocalDateTime.now());

        TokenInfo refreshTokenInfo = jwtTokenProvider.parseToken(response.refreshToken());
        assertThat(refreshTokenInfo.subject()).isEqualTo(member.id().toString());
        assertThat(refreshTokenInfo.expiresAt()).isAfter(LocalDateTime.now());
    }
}
