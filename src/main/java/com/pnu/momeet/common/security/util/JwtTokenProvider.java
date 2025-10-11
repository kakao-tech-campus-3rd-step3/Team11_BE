package com.pnu.momeet.common.security.util;

import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.model.enums.TokenType;
import com.pnu.momeet.common.security.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final String issuer;
    private final Long accessTokenExpirationSeconds;
    private final Long refreshTokenExpirationSeconds;
    private final Long upgradeTokenExpirationSeconds;

    private static final String TOKEN_TYPE_CLAIM = "type";

    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(securityProperties.getJwt().getSecret().getBytes());
        this.issuer = securityProperties.getJwt().getIssuer();
        this.accessTokenExpirationSeconds = (long) securityProperties.getJwt().getAccessToken().getExpirationInSecond();
        this.refreshTokenExpirationSeconds = (long) securityProperties.getJwt().getRefreshToken().getExpirationInSecond();
        this.upgradeTokenExpirationSeconds = (long) securityProperties.getJwt().getWsUpgradeToken().getExpirationInSecond();
    }

    public String generateToken(String sub, Long expirationSeconds, TokenType tokenType) {
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiredAt = issuedAt.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(sub)
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(expiredAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(secretKey)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .compact();
    }

    public String generateAccessToken(UUID memberId) {
        return generateToken(memberId.toString(), accessTokenExpirationSeconds, TokenType.ACCESS);
    }

    public String generateRefreshToken(UUID memberId) {
        return generateToken(memberId.toString(), refreshTokenExpirationSeconds, TokenType.REFRESH);
    }

    public String generateWsUpgradeToken(UUID memberId) {
        return generateToken(memberId.toString(), upgradeTokenExpirationSeconds, TokenType.WS_UPGRADE);
    }

    public Jws<Claims> getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
    }

    public Claims getPayload(String token) {
        return getClaims(token).getPayload();
    }

    public TokenInfo parseToken(String token) {
        Claims claims = getPayload(token);
        TokenType tokenType = TokenType.valueOf(claims.get(TOKEN_TYPE_CLAIM, String.class));
        return new TokenInfo(
                claims.getSubject(),
                claims.getIssuedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                tokenType
        );
    }
}
