package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.profile.dto.ProfileCreateRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProfileCreateTest extends BaseProfileTest {

    @Test
    @DisplayName("프로필 생성 성공 - 201 Created")
    void createMyProfile_success() {
        // admin 계정은 프로필이 없다고 가정
        ProfileCreateRequest request = new ProfileCreateRequest(
            "새로운닉네임",
            25,
            "MALE",
            "서울 강남구",
            "http://example.com/profile.jpg",
            "새로운 자기소개입니다."
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then().log().all()
            .statusCode(201)
            .header("Location", "/api/profiles/me")
            .body("nickname", equalTo("새로운닉네임"))
            .body("age", equalTo(25))
            .body("id", notNullValue());
    }

    @Test
    @DisplayName("프로필 생성 실패 - 401 Unauthorized (토큰 없음)")
    void createMyProfile_fail_unauthorized() {
        ProfileCreateRequest request = new ProfileCreateRequest(
            "닉네임",
            20,
            "FEMALE",
            "부산",
            "url",
            "소개"
        );

        RestAssured
            .given().log().all()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then().log().all()
            .statusCode(401);
    }

    @Test
    @DisplayName("프로필 생성 실패 - 400 Bad Request (유효성 검사 실패)")
    void createMyProfile_fail_validation() {
        // 닉네임을 일부러 짧게 만들어 유효성 검사 실패 유도
        ProfileCreateRequest request = new ProfileCreateRequest(
            "닉",
            20,
            "FEMALE",
            "부산",
            "url",
            "소개"
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then().log().all()
            .statusCode(400);
    }

    @Test
    @DisplayName("프로필 생성 실패 - 409 Conflict (프로필이 이미 존재)")
    void createMyProfile_fail_conflict() {
        // ROLE_USER 계정은 프로필이 이미 존재한다고 가정
        ProfileCreateRequest request = new ProfileCreateRequest(
            "테스트유저",
            30,
            "MALE",
            "경기",
            "url",
            "소개"
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post()
            .then().log().all()
            .statusCode(409)
            .body("detail", containsString("프로필이 이미 존재합니다."));
    }
}

