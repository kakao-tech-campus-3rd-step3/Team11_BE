package com.pnu.momeet.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {
    private List<String> whitelistUrls;
    private Jwt jwt;
    private Https https;

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private AccessToken accessToken;
        private RefreshToken refreshToken;
        private String issuer;

        @Getter
        @Setter
        public static class AccessToken {
            private int expirationInSecond;
        }

        @Getter
        @Setter
        public static class RefreshToken {
            private int expirationInSecond;
        }
    }

    @Getter
    @Setter
    public static class Https {
        private boolean enabled;
        private String domain;
        private List<String> allowOrigins;
    }
}
