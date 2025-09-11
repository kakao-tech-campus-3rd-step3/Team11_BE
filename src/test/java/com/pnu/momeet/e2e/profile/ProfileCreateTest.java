package com.pnu.momeet.e2e.profile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.member.enums.Role;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

public class ProfileCreateTest extends BaseProfileTest {

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
    @DisplayName("프로필 생성 성공 - 201 Created")
    void createMyProfile_success() throws IOException {
        String fakeImageUrl = "https://cdn.example.com/profiles/new-uuid.png";
        given(s3StorageService.uploadImage(any(), eq("/profiles"))).willReturn(fakeImageUrl);

        ClassPathResource resource = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = resource.getInputStream().readAllBytes();

        // 한글 필드의 인코딩을 명시적으로 지정하여 테스트의 안정성 확보
        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("새로운닉네임")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification descriptionPart = new MultiPartSpecBuilder("새로운 자기소개입니다.")
            .controlName("description").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification baseLocationPart = new MultiPartSpecBuilder("서울 강남구")
            .controlName("baseLocation").charset(StandardCharsets.UTF_8).build();

        ExtractableResponse<Response> response = RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART) // multipart/form-data 사용
            .multiPart(nicknamePart)
            .multiPart("age", 25)
            .multiPart("gender", "MALE")
            .multiPart(descriptionPart)
            .multiPart(baseLocationPart)
            .multiPart("image", "/image/badger.png", imageBytes, "image/png") // 이미지 파일 추가
            .when()
            .post()
            .then().log().all()
            .statusCode(201)
            .header("Location", containsString("/api/profiles/"))
            .body("nickname", equalTo("새로운닉네임"))
            .body("age", equalTo(25))
            .body("id", notNullValue())
            .body("imageUrl", equalTo(fakeImageUrl)) // 응답에 이미지 URL이 포함되었는지 검증
            .extract();

        UUID createdProfileId = UUID.fromString(response.jsonPath().getString("id"));
        profilesToBeDeleted.add(createdProfileId);
    }

    @Test
    @DisplayName("프로필 생성 실패 - 401 Unauthorized (토큰 없음)")
    void createMyProfile_fail_unauthorized() {
        // 이 테스트는 이미지 없이 Content-Type만 multipart로 보내도 무방
        RestAssured
            .given().log().all()
            .contentType(ContentType.MULTIPART)
            .multiPart("nickname", "닉네임")
            .when()
            .post()
            .then().log().all()
            .statusCode(401);
    }

    @Test
    @DisplayName("프로필 생성 실패 - 400 Bad Request (유효성 검사 실패)")
    void createMyProfile_fail_validation() {
        // 닉네임을 일부러 짧게 만들어 유효성 검사 실패 유도
        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_ADMIN).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart("nickname", "닉") // 유효성 검사 실패 조건
            .multiPart("age", 20)
            .multiPart("gender", "FEMALE")
            .when()
            .post()
            .then().log().all()
            .statusCode(400);
    }

    @Test
    @DisplayName("프로필 생성 실패 - 409 Conflict (프로필이 이미 존재)")
    void createMyProfile_fail_conflict() throws Exception {
        // ROLE_USER 계정은 프로필이 이미 존재한다고 가정
        ClassPathResource resource = new ClassPathResource("/image/badger.png");
        byte[] imageBytes = resource.getInputStream().readAllBytes();

        MultiPartSpecification nicknamePart = new MultiPartSpecBuilder("테스트유저")
            .controlName("nickname").charset(StandardCharsets.UTF_8).build();
        MultiPartSpecification baseLocationPart = new MultiPartSpecBuilder("서울 강남구")
            .controlName("baseLocation").charset(StandardCharsets.UTF_8).build();

        RestAssured
            .given().log().all()
            .header(AUTH_HEADER, BEAR_PREFIX + getToken(Role.ROLE_USER).accessToken())
            .contentType(ContentType.MULTIPART)
            .multiPart(nicknamePart)
            .multiPart("age", 30)
            .multiPart("gender", "MALE")
            .multiPart(baseLocationPart)
            .multiPart("image", "/image/badger.png", imageBytes, "image/png")
            .when()
            .post()
            .then().log().all()
            .statusCode(409)
            .body("detail", containsString("프로필이 이미 존재합니다."));
    }
}
