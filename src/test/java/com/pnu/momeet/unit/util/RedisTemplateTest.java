package com.pnu.momeet.unit.util;

import com.pnu.momeet.domain.auth.dto.persistence.VerificationCode;
import com.pnu.momeet.domain.auth.repository.VerificationCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private RedisTemplate<String, VerificationCode> verificationCodeRedisTemplate;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Test
    public void testVerificationCodeRedisTemplate() throws NullPointerException{
        UUID memberId = UUID.randomUUID();
        VerificationCode code = VerificationCode.generate(memberId);
        System.out.println("Generated Code: " + code);
        String key = "VERIFICATION_CODE::" + code.body();
        // Save using verificationCodeRedisTemplate
        verificationCodeRedisTemplate.opsForValue().set(key, code);

        // Retrieve using verificationCodeRedisTemplate
        VerificationCode retrievedCode = verificationCodeRedisTemplate.opsForValue().get(key);
        assertThat(retrievedCode).isNotNull();
        System.out.println("Retrieved Code: " + retrievedCode);
        assertThat(retrievedCode.body()).isEqualTo(code.body());
        assertThat(retrievedCode.memberId()).isEqualTo(memberId.toString());

        // Clean up
        verificationCodeRedisTemplate.delete(key);
    }

    @Test
    public void testVerificationCodeRepository() {
        UUID memberId = UUID.randomUUID();
        VerificationCode code = VerificationCode.generate(memberId);
        System.out.println("Generated Code: " + code);

        // Save the verification code
        verificationCodeRepository.save(code);

        // Retrieve the verification code
        var retrievedOpt = verificationCodeRepository.findByBody(code.body());
        assertThat(retrievedOpt).isPresent();
        VerificationCode retrievedCode = retrievedOpt.get();
        assertThat(retrievedCode.memberId()).isEqualTo(memberId.toString());

        // Delete the verification code
        verificationCodeRepository.delete(code);
        var deletedOpt = verificationCodeRepository.findByBody(code.body());
        assertThat(deletedOpt).isNotPresent();
    }
}
