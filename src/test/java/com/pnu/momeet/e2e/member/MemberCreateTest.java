package com.pnu.momeet.e2e.member;

import com.pnu.momeet.domain.member.dto.MemberCreateRequest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

@Tag("create")
public class MemberCreateTest extends BaseMemberTest {

    @Test
    public void create_member_success() {

        MemberCreateRequest request = new MemberCreateRequest(
                "test1234@test.com",
                "testpass1234!",
                List.of("ROLE_USER")
        );

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .contentType("application/json")
                .body(request)
            .when()
                .post()
            .then()
                .log().all()
                .statusCode(201);
    }
}
