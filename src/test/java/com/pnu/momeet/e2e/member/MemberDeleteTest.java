package com.pnu.momeet.e2e.member;

import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@Tag("delete")
@DisplayName("E2E : Member 삭제 테스트")
public class MemberDeleteTest extends BaseMemberTest {

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        MemberResponse member = memberService.saveMember(
            new Member(
                "delete_test1@test.com",
                "deleteTestPass1!",
                List.of(Role.ROLE_USER)
            )
        );
        toBeDeleted.add(member.id());
    }

    @Test
    @DisplayName("멤버 삭제 성공 테스트 - 204 No Content")
    public void delete_Member_success() {
        UUID memberId = toBeDeleted.removeLast(); // 삭제할 멤버 ID 가져오기

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
            .when()
                .delete("/{id}", memberId)
            .then()
                .log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("멤버 삭제 실패 테스트 - 404 Not Found - 존재하지 않는 멤버")
    public void delete_Member_fail_by_not_found() {
        UUID nonExistentId = UUID.randomUUID(); // 존재하지 않는 멤버 ID

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
            .when()
                .delete("/{id}", nonExistentId)
            .then()
                .log().all()
                .statusCode(404);
    }

}
