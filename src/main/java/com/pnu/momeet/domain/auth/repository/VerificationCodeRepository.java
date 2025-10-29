package com.pnu.momeet.domain.auth.repository;

import com.pnu.momeet.common.security.config.SecurityProperties;
import com.pnu.momeet.domain.auth.dto.persistence.VerificationCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class VerificationCodeRepository {
    private static final String PREFIX = "VERIFICATION_CODE::";
    private final Long ttl;
    private final RedisTemplate<String, VerificationCode> redisTemplate;

    public VerificationCodeRepository(
            SecurityProperties securityProperties,
            RedisTemplate<String, VerificationCode> redisTemplate
    ) {
        this.ttl = securityProperties.getVerification().getExpirationInMinute() * 60L;
        this.redisTemplate = redisTemplate;
    }

    private String getKey(String body) {
        return PREFIX + body;
    }

    public void save(VerificationCode code) {
        String key = getKey(code.body());
        redisTemplate.opsForValue().set(key, code, ttl, TimeUnit.SECONDS);
    }

    public Optional<VerificationCode> findByBody(String body) {
        String key = getKey(body);
        VerificationCode code = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(code);
    }

    public void deleteByBody(String body) {
        String key = getKey(body);
        redisTemplate.delete(key);
    }

    public void delete(VerificationCode code) {
        deleteByBody(code.body());
    }

}
