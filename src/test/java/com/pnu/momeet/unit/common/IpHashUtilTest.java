package com.pnu.momeet.unit.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pnu.momeet.common.exception.IpHashGenerationException;
import com.pnu.momeet.common.util.IpHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class IpHashUtilTest {

    @InjectMocks private IpHashUtil sut;

    @BeforeEach
    void setUp() {
        // @Value 주입 필드 강제로 세팅
        ReflectionTestUtils.setField(sut, "SECRET_KEY", "test-secret");
        ReflectionTestUtils.setField(sut, "HASH_METHOD", "SHA-256");
    }

    private HttpServletRequest mockRequest(String ip) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(ip);
        return req;
    }

    @Test
    @DisplayName("같은 IP는 같은 해시값을 반환한다")
    void sameIp_sameHash() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1");

        String hash1 = sut.fromRequest(request);
        String hash2 = sut.fromRequest(request);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 IP는 다른 해시값을 반환한다")
    void differentIp_differentHash() {
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        when(req1.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1");

        HttpServletRequest req2 = mock(HttpServletRequest.class);
        when(req2.getHeader("X-Forwarded-For")).thenReturn("192.168.0.2");

        String hash1 = sut.fromRequest(req1);
        String hash2 = sut.fromRequest(req2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 없으면 RemoteAddr을 사용한다")
    void fallbackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String hash = sut.fromRequest(request);

        assertThat(hash).isEqualTo(sut.fromRequest(request)); // 같은 입력 → 같은 해시
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 여러 개면 첫 번째 값을 사용한다")
    void xForwardedFor_multipleIps_useFirst() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For"))
            .thenReturn("203.0.113.10, 10.0.0.1");

        String hash = sut.fromRequest(request);

        // 첫 번째 값(203.0.113.10)만 사용했는지 검증
        String expected = sut.fromRequest(mockRequest("203.0.113.10"));
        assertThat(hash).isEqualTo(expected);
    }
}

