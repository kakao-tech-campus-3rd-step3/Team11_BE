package com.pnu.momeet.e2e.badge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("badge")
public class BadgeGetTest extends BaseBadgeTest {

    @Test
    @DisplayName("전체 배지 조회 - ADMIN 성공 (기본 정렬: createdAt,desc)")
    void getAllBadges_admin_success_defaultSort() {
        // given
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        // when & then
        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            // sort 미지정 → 기본 createdAt,desc 적용
            .when()
            .get()
            .then()
            .log().all()
            .statusCode(200)
            .body("content", notNullValue())
            // Page 메타 검증 (프로젝트 공통 포맷에 맞춰 필드명 조정 가능)
            .body("page.number", equalTo(0))
            .body("page.size", equalTo(10))
            .body("page.totalElements", notNullValue())
            .body("page.totalPages", notNullValue())
            .body("content[0].name", equalTo("[TEST] 호감 인기인"))
            .body("content[0].description", equalTo("테스트용: 좋아요 10개"))
            .body("content[0].code", equalTo("LIKE_10"))
            .body("content[1].name", equalTo("[TEST] 모임 고수"))
            .body("content[1].description", equalTo("테스트용: 10회 참여 배지"))
            .body("content[1].code", equalTo("TEN_JOINS"))
            .body("content[2].name", equalTo("[TEST] 모임 새싹"))
            .body("content[2].description", equalTo("테스트용: 첫 참여 배지"))
            .body("content[2].code", equalTo("FIRST_JOIN"));
    }

    @Test
    @DisplayName("전체 배지 조회 - ADMIN 성공 (허용 정렬: name,asc)")
    void getAllBadges_admin_success_nameAsc() {
        // given
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .queryParam("page", 0)
            .queryParam("size", 5)
            .queryParam("sort", "name,asc")
            .when()
            .get()
            .then()
            .log().all()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page.number", equalTo(0))
            .body("page.size", equalTo(5))
            .body("page.totalElements", equalTo(3))
            .body("page.totalPages", equalTo(1))
            .body("content[0].name", equalTo("[TEST] 모임 고수"))
            .body("content[0].description", equalTo("테스트용: 10회 참여 배지"))
            .body("content[0].code", equalTo("TEN_JOINS"))
            .body("content[1].name", equalTo("[TEST] 모임 새싹"))
            .body("content[1].description", equalTo("테스트용: 첫 참여 배지"))
            .body("content[1].code", equalTo("FIRST_JOIN"))
            .body("content[2].name", equalTo("[TEST] 호감 인기인"))
            .body("content[2].description", equalTo("테스트용: 좋아요 10개"))
            .body("content[2].code", equalTo("LIKE_10"));
    }

    @Test
    @DisplayName("전체 배지 조회 - USER 접근 금지(403) (관리자 전용)")
    void getAllBadges_user_forbidden() {
        // given
        String userToken = getToken(Role.ROLE_USER).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + userToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get()
            .then()
            .log().all()
            .statusCode(403);
    }

    @Test
    @DisplayName("전체 배지 조회 - ADMIN 잘못된 정렬 키로 요청 시 400")
    void getAllBadges_admin_invalidSort_fallbackToDefault() {
        String adminToken = getToken(Role.ROLE_ADMIN).accessToken();

        RestAssured
            .given()
            .header(AUTH_HEADER, BEAR_PREFIX + adminToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sort", "hacker,desc")
            .when()
            .get()
            .then()
            .log().all()
            .statusCode(400)
            .body("validationErrors.sort[0]",
                equalTo("필드 정렬 조건이 올바르지 않습니다. ex)field1,asc,field2,desc (허용된 필드: createdAt, name)"));
    }
}
