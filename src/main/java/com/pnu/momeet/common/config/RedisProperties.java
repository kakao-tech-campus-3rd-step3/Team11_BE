package com.pnu.momeet.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.redis")
@Getter
@Setter
public class RedisProperties {
    private String host;
    private int port;
}
