package com.pnu.momeet.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class GeneratorTest {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordEncoding() {
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
    }

    @Test
    public void testUUIDGeneration() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Generated UUID: " + java.util.UUID.randomUUID());
        }
    }
}
