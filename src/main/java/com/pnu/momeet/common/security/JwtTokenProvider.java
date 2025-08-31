package com.pnu.momeet.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
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

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer}") String issuer,
        @Value("${jwt.access-token.expiration}") Long accessTokenExpirationSeconds,
        @Value("${jwt.refresh-token.expiration}") Long refreshTokenExpirationSeconds
    ) {
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String generateToken(String sub, Long expirationSeconds) {
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(sub)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(Date.from(expiredAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(secretKey)
                .compact();
    }

    public String generateAccessToken(UUID memberId) {
        return generateToken(memberId.toString(), accessTokenExpirationSeconds);
    }

    public String generateRefreshToken(UUID memberId) {
        return generateToken(memberId.toString(), refreshTokenExpirationSeconds);
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

    public Claims getPayloadWithoutExpiration(String token) {
        try {
            return getClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims(); // 토큰이 만료되어도 페이로드는 반환
        }
    }
}
