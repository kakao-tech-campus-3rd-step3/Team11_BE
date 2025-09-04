package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.equalTo;

import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import com.pnu.momeet.domain.profile.dto.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class ProfileUpdateTest extends BaseProfileTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    @DisplayName("프로필 수정 성공 - 200 OK")
    void updateMyProfile_success() {
        Member testUser = new Member(
            "test.update@test.com",
            "pass",
            List.of(Role.ROLE_USER)
        );
        memberRepository.save(testUser);
        membersToBeDeleted.add(testUser.getId());

        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "수정된닉네임",
            30,
            "FEMALE",
            null,
            "수정된소개",
            null
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("nickname", equalTo("수정된닉네임"))
            .body("age", equalTo(30))
            .body("gender", equalTo("FEMALE"))
            .body("description", equalTo("수정된소개"));
    }

    @Test
    @DisplayName("프로필 수정 실패 - 401 Unauthorized (토큰 없음)")
    void updateMyProfile_fail_unauthorized() {
        Member anotherUser = new Member(
            "another@test.com",
            "pass",
            List.of(Role.ROLE_USER)
        );
        memberRepository.saveAndFlush(anotherUser);
        membersToBeDeleted.add(anotherUser.getId());
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "수정",
            30,
            null,
            null,
            null,
            null
        );

        RestAssured
            .given().log().all()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 404 Not Found (수정할 프로필 없음)")
    void updateMyProfile_fail_profileNotFound() {
        // admin 계정은 프로필이 없다고 가정
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "수정",
            30,
            null,
            null,
            null,
            null
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 400 BAD_REQUEST (닉네임 중복)")
    void updateMyProfile_fail_duplicateNickname() {
        // 중복될 닉네임을 가진 다른 유저의 프로필을 생성
        Member anotherUser = new Member(
            "another@test.com",
            "pass",
            List.of(Role.ROLE_USER)
        );
        memberRepository.save(anotherUser);
        membersToBeDeleted.add(anotherUser.getId());
        Profile conflictingProfile = Profile.create(
            anotherUser.getId(),
            "중복된닉네임",
            20,
            Gender.FEMALE,
            "url",
            "소개",
            "장소"
        );
        profileRepository.save(conflictingProfile);

        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "중복된닉네임",
            30,
            null,
            null,
            null,
            null
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 400 Bad Request (유효성 검사 실패)")
    void updateMyProfile_fail_validation() {
        // 닉네임을 1글자로 보내 유효성 검사 실패 유도
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "닉",
            30,
            null,
            null,
            null,
            null
        );

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}