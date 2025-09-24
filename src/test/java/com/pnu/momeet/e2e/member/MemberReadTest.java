package com.pnu.momeet.e2e.member;

import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;

@Tag("read")
@DisplayName("E2E : Member 조회 테스트")
public class MemberReadTest extends BaseMemberTest {

    private MemberResponse testMember;
    private String testMemberToken;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();

        IntStream.range(0, 15)
            .forEach(num -> {
                var request = new MemberCreateRequest(
                    "readTest" + num + "@test.com",
                    "ReadTestPass123!@#",
                    List.of(Role.ROLE_USER.name())
                );
                var response =  memberService.saveMember(request);
                toBeDeleted.add(response.id());
            });

        testMember = memberService.saveMember(
                new MemberCreateRequest(
                "testerRead@test.com",
                "ReadTestPass123!@#",
                List.of(Role.ROLE_USER.name())
        ));

        testMemberToken = emailAuthService.login(
                testMember.email(),
                "ReadTestPass123!@#"
        ).accessToken();

        toBeDeleted.add(testMember.id());

    }

    @Test
    @DisplayName("멤버 목록 조회 성공 - 200 OK")
    public void read_member_list_success() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken()).queryParam("page", 0)
                .queryParam("size", 5)
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "content.size()", equalTo(5),
                    "page.totalElements", greaterThanOrEqualTo(15),
                    "page.totalPages", greaterThanOrEqualTo(3),
                    "page.number", equalTo(0),
                    "page.size", equalTo(5),
                    "content[0].id", notNullValue()
                );
    }

    @Test
    @DisplayName("멤버 목록 조회 성공(sort by createdAt desc) - 200 OK")
    public void read_member_list_success_sorted_by_createdAt_desc() {
        List<MemberResponse> page = RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sort", "createdAt,desc")
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "content.size()", equalTo(5),
                    "page.totalElements", greaterThanOrEqualTo(15),
                    "page.totalPages", greaterThanOrEqualTo(3),
                    "page.number", equalTo(0),
                    "page.size", equalTo(5),
                    "content[0].id", notNullValue()
                )
                .extract()
                .jsonPath()
                .getList("content", MemberResponse.class);
        // createdAt desc 정렬 확인
        for (int i = 1; i < page.size(); i++) {
            MemberResponse prev = page.get(i - 1);
            MemberResponse curr = page.get(i);
            assert prev.createdAt().isAfter(curr.createdAt()) || prev.createdAt().isEqual(curr.createdAt());
        }
    }

    @Test
    @DisplayName("멤버 목록 조회 실패 - 400 Bad Request (유효성 검사 실패)")
    public void read_member_list_fail_validation_fails() {
        List.of(
            Map.of("page", 0, "size", 0),   // size 0
            Map.of("page", -1, "size", 5),   // page -1
            Map.of("sort", "invalidField,asc") // 정렬 필드 잘못됨
        ).forEach(params ->
            RestAssured
                .given()
                    .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                    .queryParams(params)
                .when()
                    .get()
                .then()
                    .log().all()
                    .statusCode(400)
        );
    }

    @Test
    @DisplayName("멤버 목록 조회 실패 - 401 Unauthorized (토큰 없음) ")
    public void read_member_list_fail_unauthorized() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
                .queryParam("page", 0)
                .queryParam("size", 5)
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("멤버 목록 조회 실패 - 403 Forbidden (권한 없음) ")
    public void read_member_list_fail_forbidden() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
                .queryParam("page", 0)
                .queryParam("size", 5)
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("멤버 단건 조회 성공 - 200 OK")
    public void read_member_success() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
            .when()
                .get("/{id}", testMember.id())
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(testMember.id().toString()),
                    "email", equalTo(testMember.email()),
                    "roles", hasItem("ROLE_USER"),
                    "createdAt", notNullValue(),
                    "updatedAt", notNullValue()
                );
    }

    @Test
    @DisplayName("멤버 단건 조회 실패 - 401 Unauthorized (토큰 없음) ")
    public void read_member_fail_unauthorized() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .get("/{id}", testMember.id())
            .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("멤버 단건 조회 실패 - 403 Forbidden (권한 없음) ")
    public void read_member_fail_forbidden() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .when()
                .get("/{id}", testMember.id())
            .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("멤버 단건 조회 실패 - 404 Not Found (존재하지 않는 멤버) ")
    public void read_member_fail_not_found() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
            .when()
                .get("/{id}", UUID.randomUUID())
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("내 정보 조회 성공 - 200 OK")
    public void read_member_self_success() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + testMemberToken)
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "id", equalTo(testMember.id().toString()),
                    "email", equalTo(testMember.email()),
                    "roles", hasItem("ROLE_USER"),
                    "createdAt", notNullValue(),
                    "updatedAt", notNullValue()
                );
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 401 Unauthorized (토큰 없음) ")
    public void read_member_self_fail_unauthorized() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + "invalid_token")
            .when()
                .get("/me")
            .then()
                .log().all()
                .statusCode(401);
    }
}
