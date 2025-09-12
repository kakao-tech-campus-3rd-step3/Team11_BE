package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

public class ProfileUpdateTest extends BaseProfileTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private S3StorageService s3StorageService;

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3UploaderService() {
            // 실제 S3 호출 방지용 mock 빈
            return Mockito.mock(S3StorageService.class);
        }
    }

    @Test
    @DisplayName("프로필 수정 성공 (새 이미지로 교체) - 200 OK")
    void updateMyProfile_withNewImage_success() throws IOException {
        // given: user1은 data.sql에 의해 기존 이미지가 있다고 가정
        String newImageUrl = "https://cdn.example.com/profiles/new-updated-uuid.png";

        // S3 Mocking: 기존 이미지는 삭제되고, 새 이미지는 업로드되도록 설정
        willDoNothing().given(s3StorageService).deleteImage(anyString());
        given(s3StorageService.uploadImage(any(), anyString())).willReturn(newImageUrl);

        // 한글 필드 인코딩 설정
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("수정된닉네임")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification descriptionPart = new MultiPartSpecBuilder("수정된 자기소개")
            .controlName("description").charset(StandardCharsets.UTF_8).build();

        // 테스트용 이미지 파일 로드
        ClassPathResource resource = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = resource.getInputStream().readAllBytes();

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart)
            .multiPart("age", 30)
            .multiPart("gender", "FEMALE")
            .multiPart(descriptionPart)
            .multiPart("image", "new_badger.png", imageBytes, "image/png")
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("nickname", equalTo("수정된닉네임"))
            .body("age", equalTo(30))
            .body("imageUrl", equalTo(newImageUrl));

        // verify: S3 서비스가 예상대로 호출되었는지 검증
        verify(s3StorageService, times(1)).deleteImage(anyString());
        verify(s3StorageService, times(1)).uploadImage(any(), anyString());
    }

    @Test
    @DisplayName("프로필 수정 성공 (이미지 변경 없음) - 200 OK")
    void updateMyProfile_withoutImage_success() {
        // given
        Mockito.reset(s3StorageService); // mock 객체 초기화

        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("닉네임만수정")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart) // 텍스트 필드만 전송
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("nickname", equalTo("닉네임만수정"))
            .body("imageUrl", notNullValue()); // 기존 이미지가 유지되는지 확인

        // verify: S3 관련 메서드가 전혀 호출되지 않았는지 검증 (중요)
        verify(s3StorageService, never()).deleteImage(anyString());
        verify(s3StorageService, never()).uploadImage(any(), anyString());
    }

    // --- 이하 실패 테스트 케이스들 (Multipart 형식으로 통일) ---

    @Test
    @DisplayName("프로필 수정 실패 - 401 Unauthorized (토큰 없음)")
    void updateMyProfile_fail_unauthorized() {
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("수정된닉네임")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 404 Not Found (수정할 프로필 없음)")
    void updateMyProfile_fail_profileNotFound() {
        // admin 계정은 프로필이 없다고 가정
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("어드민")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 400 BAD_REQUEST (닉네임 중복)")
    void updateMyProfile_fail_duplicateNickname() {
        // given: 다른 사용자가 사용할 닉네임을 미리 선점
        String duplicateNickname = "중복된닉네임";
        Member anotherMember = new Member(
            "another@test.com",
            "password",
            List.of(Role.ROLE_USER)
        );
        memberRepository.save(anotherMember);
        membersToBeDeleted.add(anotherMember.getId());

        Profile anotherProfile = Profile.create(
            anotherMember.getId(),
            duplicateNickname,
            20,
            Gender.FEMALE,
            null,
            "desc",
            "loc"
        );
        profileRepository.save(anotherProfile);
        profilesToBeDeleted.add(anotherProfile.getId());

        // when: ROLE_USER가 선점된 닉네임으로 변경 시도
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder(duplicateNickname)
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart)
            .when()
            .patch("/me")
            // then: 400 Bad Request가 발생해야 함
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());

    }

    @Test
    @DisplayName("프로필 수정 실패 - 400 Bad Request (유효성 검사 실패)")
    void updateMyProfile_fail_validation() {
        // 닉네임을 1글자로 보내 유효성 검사 실패 유도
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("닉")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}