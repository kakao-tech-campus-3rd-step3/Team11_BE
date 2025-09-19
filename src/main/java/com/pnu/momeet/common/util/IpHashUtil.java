package com.pnu.momeet.common.util;

import com.pnu.momeet.common.exception.IpHashGenerationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IpHashUtil {

    @Value("${ip-hash.secret-key}")
    private String SECRET_KEY;

    @Value("${ip-hash.method}")
    private String HASH_METHOD;

    // TODO [운영 단계 보강]
    // - Nginx/ALB에서 X-Forwarded-For 헤더 강제 세팅 (헤더 조작 방지)
    // - Spring Boot `forward-headers-strategy=framework` 활성화 → getRemoteAddr()로 간단히 사용 가능
    // - Redis 기반 rate limiting 추가 (예: 신고/평가 API 1시간 10회 제한)

    public String fromRequest(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        return generateHash(clientIp);
    }

    private String generateHash(String ip) {
        try {
            String normalized = normalizeIp(ip);
            String input = normalized + SECRET_KEY;

            MessageDigest digest = MessageDigest.getInstance(HASH_METHOD);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IpHashGenerationException("IP 해시 생성 실패");
        }
    }

    private String normalizeIp(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.getHostAddress();
        } catch (UnknownHostException e) {
            return ip;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
