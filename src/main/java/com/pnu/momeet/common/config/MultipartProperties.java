package com.pnu.momeet.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.servlet.multipart")
public class MultipartProperties {
    private DataSize maxFileSize;
    private DataSize maxRequestSize;
}
