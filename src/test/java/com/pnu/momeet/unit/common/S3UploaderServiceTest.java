package com.pnu.momeet.unit.common;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import com.pnu.momeet.common.service.S3UploaderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class S3UploaderServiceTest {

    @Mock private S3Client s3Client;
    @Mock private S3Utilities s3Utilities;

    @InjectMocks private S3UploaderService sut;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        // @Value 주입 필드 세팅
        ReflectionTestUtils.setField(sut, "bucket", BUCKET);
    }

    // ---- helpers ----

    private static byte[] pngBytes(int w, int h) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 성공 케이스에서만 호출: utilities().getUrl(Consumer<Builder>) 스텁 */
    private void stubGetUrlToDummy() {
        when(s3Client.utilities()).thenReturn(s3Utilities);
        // Consumer 오버로드로 명확히 스텁 (모호성/strict stubbing 방지)
        doReturn(url("https://test-bucket.s3.amazonaws.com/dummy"))
            .when(s3Utilities).getUrl(any(Consumer.class));
    }

    private URL url(String s) {
        try { return new URL(s); } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ---- tests ----

    @Test
    @DisplayName("성공 - 정상 PNG 업로드 시 URL 반환 및 PutObjectRequest 검증")
    void upload_success_png() {
        // given
        stubGetUrlToDummy();
        byte[] bytes = pngBytes(2, 2);
        MockMultipartFile file = new MockMultipartFile(
            "image", "avatar.PNG", "image/png", bytes); // 대문자 확장자도 허용
        String prefix = "profiles";

        // when
        String url = sut.uploadImage(file, prefix);

        // then
        assertThat(url).startsWith("https://test-bucket.s3.amazonaws.com/");

        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        PutObjectRequest req = cap.getValue();

        assertThat(req.bucket()).isEqualTo(BUCKET);
        assertThat(req.acl()).isEqualTo(ObjectCannedACL.PUBLIC_READ); // TODO: 운영 시 제거 예정
        assertThat(req.contentType()).isEqualTo("image/png");
        assertThat(req.cacheControl()).contains("max-age");
        assertThat(req.key()).startsWith(prefix + "/");
        assertThat(req.key().toLowerCase(Locale.ROOT)).endsWith(".png");
    }

    @Test
    @DisplayName("성공 - JPG는 image/jpeg 로 매핑된다")
    void upload_success_jpg_mime() {
        stubGetUrlToDummy();
        byte[] bytes = pngBytes(1, 1); // 내용은 PNG여도 디코딩만 되면 테스트 단순화 목적 OK
        MockMultipartFile file = new MockMultipartFile(
            "image", "face.jpg", "image/jpeg", bytes);

        sut.uploadImage(file, "images");

        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        assertThat(cap.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Nested
    class ValidationFailures {

        @Test
        @DisplayName("실패 - 파일명이 비어있으면 400(IllegalArgumentException)")
        void fail_empty_filename() {
            MockMultipartFile file = new MockMultipartFile(
                "image", "", "image/png", pngBytes(1, 1));

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 이름은 비어 있을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 확장자 없음")
        void fail_no_extension() {
            MockMultipartFile file = new MockMultipartFile(
                "image", "noext", "image/png", pngBytes(1, 1));

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("확장자가 존재해야 합니다");
        }

        @Test
        @DisplayName("실패 - 허용되지 않은 확장자")
        void fail_disallowed_extension() {
            MockMultipartFile file = new MockMultipartFile(
                "image", "evil.exe", "application/octet-stream", new byte[] {1,2,3});

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않은 확장자");
        }

        @Test
        @DisplayName("실패 - Content-Type 이 image/* 아님")
        void fail_wrong_content_type() {
            MockMultipartFile file = new MockMultipartFile(
                "image", "a.png", "text/plain", pngBytes(1, 1));

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 Content-Type이 아닙니다");
        }

        @Test
        @DisplayName("실패 - 디코딩 불가(이미지가 아님)")
        void fail_not_image() {
            MockMultipartFile file = new MockMultipartFile(
                "image", "cat.png", "image/png", new byte[] {0,1,2,3}); // 깨진 바이너리

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 파일이 아닙니다");
        }

        @Test
        @DisplayName("실패 - 파일이 비어있음")
        void fail_empty_file() {
            MockMultipartFile file = new MockMultipartFile(
                "image", "a.png", "image/png", new byte[] {});

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일이 비어있습니다");
        }

        @Test
        @DisplayName("실패 - 5MB 초과")
        void fail_over_5mb() {
            // 작은 PNG 이미지를 만든다
            byte[] smallPng = pngBytes(10, 10); // helper 메서드 활용
            // 크기를 억지로 키운다 (내용은 그대로 이미지로 간주됨)
            byte[] big = new byte[(int)(5 * 1024 * 1024) + 1];
            System.arraycopy(smallPng, 0, big, 0, smallPng.length);

            MockMultipartFile file = new MockMultipartFile(
                "image", "big.png", "image/png", big
            );

            assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB를 초과");
        }
    }

    @Test
    @DisplayName("실패 - S3 예외 발생 시 IllegalStateException 변환")
    void fail_s3_exception() {
        // given
        byte[] bytes = pngBytes(1, 1);
        MockMultipartFile file = new MockMultipartFile(
            "image", "ok.png", "image/png", bytes);

        // putObject에서 S3Exception 던지도록 모킹
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("boom").build());

        // expect
        assertThatThrownBy(() -> sut.uploadImage(file, "profiles"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("파일 저장 중 오류");
    }

    @Test
    @DisplayName("키는 prefix/UUID.ext 형식을 따르며 원본 파일명은 포함되지 않는다")
    void key_does_not_include_original_name() {
        // given
        stubGetUrlToDummy();
        byte[] bytes = pngBytes(1, 1);
        MockMultipartFile file = new MockMultipartFile(
            "image", "My Pretty Cat .png", "image/png", bytes);

        // when
        sut.uploadImage(file, "images");

        // then
        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        String key = cap.getValue().key();

        assertThat(key).startsWith("images/");
        assertThat(key.toLowerCase(Locale.ROOT)).endsWith(".png");
        assertThat(key).doesNotContain("My Pretty Cat"); // 원본 파일명 미포함
        assertThat(key.split("/")).hasSize(2); // images/{uuid.ext}
    }

    @Test
    @DisplayName("prefix 정규화 - 중복 슬래시/역슬래시는 정리되고 선행 슬래시는 제거된다")
    void prefix_normalized() {
        // given
        stubGetUrlToDummy();
        byte[] bytes = pngBytes(1, 1);
        MockMultipartFile file = new MockMultipartFile(
            "image", "a.png", "image/png", bytes);

        // when
        sut.uploadImage(file, "///profiles\\\\\\"); // 지저분한 prefix

        // then
        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        String key = cap.getValue().key();

        // 서비스의 buildKey에서 선행 "/" 제거 로직을 적용했다는 가정 하에 검증
        assertThat(key).startsWith("profiles/");
        assertThat(key).doesNotStartWith("/profiles/");
    }
}

