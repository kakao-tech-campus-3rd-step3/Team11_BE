package com.pnu.momeet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class MoMeetApplicationTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        String rawPassword = "testpass1212!";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Encoded Password: " + encodedPassword);
    }
}
