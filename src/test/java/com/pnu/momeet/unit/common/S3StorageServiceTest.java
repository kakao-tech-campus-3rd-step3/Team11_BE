package com.pnu.momeet.unit.common;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.pnu.momeet.common.exception.StorageException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import com.pnu.momeet.common.service.S3StorageService;

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

import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock private S3Client s3Client;
    @Mock private S3Utilities s3Utilities;

    @InjectMocks private S3StorageService sut;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        // @Value 주입 필드 세팅
        ReflectionTestUtils.setField(sut, "bucket", BUCKET);
        // CloudFront(또는 프론트) 도메인 주입 유지
        ReflectionTestUtils.setField(sut, "cdnBaseUrl", "https://www.momeet.click");
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

    private URL url(String s) {
        try { return new URL(s); } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ---- tests ----

    @Test
    @DisplayName("성공 - 정상 PNG 업로드 시 URL 반환 및 PutObjectRequest 검증")
    void upload_success_png() {
        // given
        byte[] bytes = pngBytes(2, 2);
        MockMultipartFile file = new MockMultipartFile(
            "image", "avatar.PNG", "image/png", bytes); // 대문자 확장자도 허용
        String prefix = "profiles";

        // when
        String url = sut.uploadImage(file, prefix);

        // then
        assertThat(url).startsWith("https://www.momeet.click/");

        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        PutObjectRequest req = cap.getValue();

        assertThat(req.bucket()).isEqualTo(BUCKET);
        assertThat(req.contentType()).isEqualTo("image/png");
        assertThat(req.cacheControl()).contains("max-age");
        assertThat(req.key()).startsWith(prefix + "/");
        assertThat(req.key().toLowerCase(Locale.ROOT)).endsWith(".png");
    }

    @Test
    @DisplayName("성공 - JPG는 image/jpeg 로 매핑된다")
    void upload_success_jpg_mime() {
        byte[] bytes = pngBytes(1, 1); // 내용은 PNG여도 디코딩만 되면 테스트 단순화 목적 OK
        MockMultipartFile file = new MockMultipartFile(
            "image", "face.jpg", "image/jpeg", bytes);

        sut.uploadImage(file, "images");

        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        assertThat(cap.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("실패 - S3 예외 발생 시 StorageException 변환")
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
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("파일 저장 중 오류");
    }

    @Test
    @DisplayName("키는 prefix/UUID.ext 형식을 따르며 원본 파일명은 포함되지 않는다")
    void key_does_not_include_original_name() {
        // given
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
        byte[] bytes = pngBytes(1, 1);
        MockMultipartFile file = new MockMultipartFile(
            "image", "a.png", "image/png", bytes);

        // when
        sut.uploadImage(file, "///profiles\\\\\\"); // 지저분한 prefix

        // then
        ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
        String key = cap.getValue().key();

        assertThat(key).startsWith("profiles/");
        assertThat(key).doesNotStartWith("/profiles/");
    }

    @Test
    @DisplayName("deleteByUrl: 퍼블릭 S3 URL에서 key를 추출해 DeleteObject 호출")
    void deleteByUrl_publicUrl() {
        S3StorageService service = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "momeet-dev-bucket-1");

        String url = "https://www.momeet.click/profiles/abc.png";
        service.deleteImage(url);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(captor.capture());

        DeleteObjectRequest req = captor.getValue();
        assertThat(req.bucket()).isEqualTo("momeet-dev-bucket-1");
        assertThat(req.key()).isEqualTo("profiles/abc.png");
    }

    @Test
    @DisplayName("deleteByUrl: CDN URL에서도 key만 올바르게 추출")
    void deleteByUrl_cdnUrl() {
        S3StorageService service = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "momeet-dev-bucket-1");

        String url = "https://cdn.momeet.app/profiles/xyz.webp";
        service.deleteImage(url);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(captor.capture());

        DeleteObjectRequest req = captor.getValue();
        // 최소 수정: 버킷 단정 오타 수정 (-1 포함)
        assertThat(req.bucket()).isEqualTo("momeet-dev-bucket-1");
        assertThat(req.key()).isEqualTo("profiles/xyz.webp");
    }

    @Test
    @DisplayName("deleteByUrl: null/빈 문자열이면 아무 것도 하지 않음(멱등)")
    void deleteByUrl_nullSafe() {
        S3StorageService service = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "momeet-dev-bucket");

        service.deleteImage(null);
        service.deleteImage("  ");

        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("deleteObjectByKey: key로 직접 삭제")
    void deleteObjectByKey() {
        S3StorageService service = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucket", "momeet-dev-bucket");

        service.deleteImage("profiles/abc.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(captor.capture());

        DeleteObjectRequest req = captor.getValue();
        assertThat(req.bucket()).isEqualTo("momeet-dev-bucket");
        assertThat(req.key()).isEqualTo("profiles/abc.png");
    }
}