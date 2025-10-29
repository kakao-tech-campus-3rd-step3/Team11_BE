package com.pnu.momeet.common.config;

import com.pnu.momeet.domain.auth.dto.persistence.VerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setUsername(redisProperties.getUsername());
        config.setPassword(redisProperties.getPassword());

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, VerificationCode> verificationCodeRedisTemplate() {
        RedisTemplate<String, VerificationCode> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setEnableTransactionSupport(true);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(VerificationCode.class));
        template.afterPropertiesSet();
        return template;
    }
}
