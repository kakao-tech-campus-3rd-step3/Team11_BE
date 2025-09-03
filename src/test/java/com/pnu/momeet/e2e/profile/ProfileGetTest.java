package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.equalTo;

import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import io.restassured.RestAssured;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class ProfileGetTest extends BaseProfileTest {

    @Autowired
    private ProfileRepository profileRepository;

    private Profile testProfile;

    @BeforeEach
    void setUp() {
        Optional<Profile> profileOptional = profileRepository.findById(TEST_USER_PROFILE_UUID);

        this.testProfile = profileOptional.orElseThrow(
            () -> new IllegalStateException("테스트용 프로필을 찾을 수 없습니다.")
        );
    }

    @Test
    @DisplayName("프로필 단건 조회 성공 - 200 OK")
    void getProfileById_success() {
        // given
        // ROLE_ADMIN으로 로그인하여 ROLE_USER의 프로필을 조회
        String accessToken = getToken(Role.ROLE_ADMIN).accessToken();

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}", testProfile.getId())
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(testProfile.getId().toString()))
            .body("nickname", equalTo(TEST_USER_PROFILE_NICKNAME));
    }

    @Test
    @DisplayName("프로필 단건 조회 실패 - 404 Not Found (존재하지 않는 프로필 ID)")
    void getProfileById_fail_notFound() {
        // given
        String accessToken = getToken(Role.ROLE_ADMIN).accessToken();
        String nonExistentProfileId = "00000000-0000-0000-0000-000000000000";

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + accessToken)
            .when()
            .get("/{profileId}", nonExistentProfileId)
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("프로필 단건 조회 실패 - 401 Unauthorized (토큰 없음)")
    void getProfileById_fail_unauthorized() {
        // when & then
        RestAssured
            .given().log().all()
            .when()
            .get("/{profileId}", testProfile.getId())
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}