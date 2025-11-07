package com.pnu.momeet.common.util;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageHashUtil {

    private static final int BUF = 8 * 1024; // 8KB

    // SHA-256(hex) 해시 계산 (스트림 기반: 대용량 파일에도 안전)
    public String sha256Hex(InputStream in) {
        try (in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[BUF];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("이미지 해시 계산 실패", e);
        }
    }

    // MultipartFile 에서 SHA-256(hex)
    public String sha256Hex(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            return sha256Hex(file.getInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("이미지 해시 계산 실패", e);
        }
    }

    // 두 해시 문자열이 동일한지(널-세이프)
    public boolean equalsHash(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}
