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
import static org.mockito.Mockito.when;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.common.util.ImageHashUtil;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

public class ProfileUpdateTest extends BaseProfileTest {

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private S3StorageService s3StorageService;

    @Autowired
    private ImageHashUtil imageHashUtil;

    @PersistenceContext
    EntityManager entityManager;

    @TestConfiguration
    static class MockS3Config {
        @Bean
        @Primary
        S3StorageService s3UploaderService() {
            // 실제 S3 호출 방지용 mock 빈
            return Mockito.mock(S3StorageService.class);
        }
    }

    private String initialImageUrl;

    @BeforeEach
    void prepareProfile() {
        // 테스트용 프로필: 현재 로그인 사용자(memberId=TEST_USER_MEMBER_UUID 가정)
        Profile me = profileRepository.findByMemberId(test_user_member_uuid).orElseThrow();
        // 초기 이미지 URL이 없다면 넣어둠(프로젝트 상황에 맞게 조정)
        if (me.getImageUrl() == null) {
            ReflectionTestUtils.setField(me, "imageUrl", "https://cdn.example.com/profiles/old.png");
            profileRepository.save(me);
        }
        initialImageUrl = me.getImageUrl();
    }

    @Test
    @DisplayName("프로필 수정 성공 - (닉네임/설명 + 새 이미지 교체) 200 OK")
    void updateProfile_success_withNewImage() throws Exception {
        // given
        String newUrl = "https://cdn.example.com/profiles/new-uuid.png";
        willDoNothing().given(s3StorageService).deleteImage(initialImageUrl);
        given(s3StorageService.uploadImage(any(), anyString())).willReturn(newUrl);

        MultiPartSpecification nickname = new MultiPartSpecBuilder("수정닉네임")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification desc = new MultiPartSpecBuilder("수정된 소개")
            .controlName("description").charset(StandardCharsets.UTF_8).build();

        byte[] imageBytes = new ClassPathResource("image/alice.png")
            .getInputStream().readAllBytes();

        TokenResponse token = getToken(Role.ROLE_USER);

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nickname)
            .multiPart(desc)
            .multiPart("image", "alice.png", imageBytes, "image/png")
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("nickname", equalTo("수정닉네임"))
            .body("description", equalTo("수정된 소개"))
            .body("imageUrl", equalTo(newUrl));

        verify(s3StorageService, times(1)).uploadImage(any(), anyString());
        verify(s3StorageService, times(1)).deleteImage(initialImageUrl);
    }

    @Test
    @DisplayName("프로필 수정 성공 - (텍스트만 변경, 이미지 미전송) 200 OK")
    void updateProfile_success_textOnly() {
        // given
        Mockito.reset(s3StorageService);
        MultiPartSpecification nickname = new MultiPartSpecBuilder("텍스트수정닉")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();

        TokenResponse token = getToken(Role.ROLE_USER);

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nickname)
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("nickname", equalTo("텍스트수정닉"))
            .body("imageUrl", equalTo(initialImageUrl)); // 기존 이미지 유지

        verify(s3StorageService, never()).uploadImage(any(), anyString());
        verify(s3StorageService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("프로필 수정 - 동일 이미지 업로드 시 S3 미호출(NO-OP)")
    void updateProfile_sameImage_skipUpload() throws Exception {
        // given: 저장된 프로필의 imageHash를 업로드할 파일과 동일하게 세팅
        byte[] same = new ClassPathResource("image/alice.png")
            .getInputStream().readAllBytes();
        String sameHash = imageHashUtil.sha256Hex(new java.io.ByteArrayInputStream(same));

        Profile me = profileRepository.findByMemberId(test_user_member_uuid).orElseThrow();
        String initialUrl = me.getImageUrl(); // 응답 검증을 위해 캡처
        ReflectionTestUtils.setField(me, "imageHash", sameHash);

        profileRepository.saveAndFlush(me); // DB 반영
        entityManager.clear();              // 1차 캐시 비움 → 서비스가 DB에서 재조회

        Mockito.reset(s3StorageService);

        TokenResponse token = getToken(Role.ROLE_USER);

        // when & then
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
            .contentType(ContentType.MULTIPART)
            // 텍스트는 기존값 그대로(변경 없음)
            .multiPart(new MultiPartSpecBuilder(me.getNickname())
                .controlName("nickname").charset(StandardCharsets.UTF_8).build())
            .multiPart(new MultiPartSpecBuilder(me.getDescription() == null ? "" : me.getDescription())
                .controlName("description").charset(StandardCharsets.UTF_8).build())
            // 동일 이미지 업로드
            .multiPart("image", "alice.png", same, "image/png")
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("imageUrl", equalTo(initialUrl)); // 동일 파일이면 URL 유지

        verify(s3StorageService, never()).uploadImage(any(), anyString());
        verify(s3StorageService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("프로필 수정 - 동일 닉네임 전달 시 NO-OP")
    void updateProfile_sameNickname_noop() {
        Profile me = profileRepository.findByMemberId(test_user_member_uuid).orElseThrow();
        TokenResponse token = getToken(Role.ROLE_USER);

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + token.accessToken())
            .contentType(ContentType.MULTIPART)
            // 파일 파트가 아닌 '텍스트 파트'로 보내기
            .multiPart(new MultiPartSpecBuilder(me.getNickname())
                .controlName("nickname").mimeType("text/plain").charset(StandardCharsets.UTF_8).build())
            .when()
            .patch("/me")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body("nickname", equalTo(me.getNickname()));
    }

    @Test
    @DisplayName("프로필 수정 실패 - 401 Unauthorized (토큰 없음)")
    void updateProfile_fail_unauthorized() {
        MultiPartSpecification nickname = new MultiPartSpecBuilder("수정시도")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .contentType(ContentType.MULTIPART)
            .multiPart(nickname)
            .when()
            .patch("/api/v1/profiles/me")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
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
        var createdMember = memberService.saveMember(new MemberCreateRequest(
                "updateFail@test.com", "updateFailPass1!", List.of("ROLE_USER")
        ));
        membersToBeDeleted.add(createdMember.id());
        var tokenResponse = emailAuthService.login("updateFail@test.com", "updateFailPass1!");
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("테스트실패유저")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + tokenResponse.accessToken())
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
        // given: 이미 존재하는 닉네임(관리자)을 중복값으로 사용
        String duplicateNickname = "관리자";

        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder(duplicateNickname)
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