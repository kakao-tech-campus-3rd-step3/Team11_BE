package com.pnu.momeet.unit.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.pnu.momeet.common.util.ImageHashUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class ImageHashUtilTest {

    private final ImageHashUtil util = new ImageHashUtil();

    private static String sha256Hex(byte[] bytes) throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(bytes));
    }

    @Mock
    MultipartFile file;

    @Test
    @DisplayName("sha256Hex(MultipartFile): 정상 스트림이면 정확한 해시를 계산한다")
    void sha256Hex_multipart_ok_withMock() throws Exception {
        byte[] data = "mock-hello".getBytes();
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(data));

        String expected = sha256Hex(data);
        String actual = util.sha256Hex(file);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("sha256Hex(MultipartFile): empty 파일이면 null을 반환한다")
    void sha256Hex_multipart_empty_returnsNull() throws Exception {
        when(file.isEmpty()).thenReturn(true);

        String actual = util.sha256Hex(file);

        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("sha256Hex(MultipartFile): getInputStream 예외는 IllegalStateException으로 래핑된다")
    void sha256Hex_multipart_ioException_wrapped() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThrows(IllegalStateException.class, () -> util.sha256Hex(file));
    }

    @Test
    @DisplayName("sha256Hex(InputStream): 스트림 예외는 IllegalStateException으로 래핑된다")
    void sha256Hex_inputStream_ioException_wrapped() {
        InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("io-error"); }
        };
        assertThrows(IllegalStateException.class, () -> util.sha256Hex(broken));
    }

    @Test
    @DisplayName("equalsHash: 널-세이프 비교가 동작한다")
    void equalsHash_nullSafe() {
        assertThat(util.equalsHash(null, null)).isTrue();
        assertThat(util.equalsHash("abc", null)).isFalse();
        assertThat(util.equalsHash(null, "abc")).isFalse();
        assertThat(util.equalsHash("abc", "abc")).isTrue();
        assertThat(util.equalsHash("abc", "ABC")).isFalse();
    }

    @Test
    @DisplayName("동일 내용(파일명 무관)의 MultipartFile 모킹: 같은 해시")
    void sameContent_sameHash_withMocks() throws Exception {
        byte[] same = "content".getBytes();

        MultipartFile f1 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile f2 = org.mockito.Mockito.mock(MultipartFile.class);

        when(f1.isEmpty()).thenReturn(false);
        when(f2.isEmpty()).thenReturn(false);
        when(f1.getInputStream()).thenReturn(new ByteArrayInputStream(same));
        when(f2.getInputStream()).thenReturn(new ByteArrayInputStream(same));

        String h1 = util.sha256Hex(f1);
        String h2 = util.sha256Hex(f2);

        assertThat(h1).isEqualTo(h2);
    }
}
