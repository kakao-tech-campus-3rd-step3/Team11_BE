package com.pnu.momeet.unit;

import com.pnu.momeet.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@SpringBootTest
public class GeneratorTest {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void testPasswordEncoding() {
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
    }

    @Test
    public void testUUIDGeneration() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Generated UUID: " + UUID.randomUUID());
        }
    }

    @Test
    public void testJwtTokenGeneration() {
        UUID userId = UUID.randomUUID();
        System.out.println("User ID: " + userId);

        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        System.out.println("Access Token: " + accessToken);

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        System.out.println("Refresh Token: " + refreshToken);
    }
}
