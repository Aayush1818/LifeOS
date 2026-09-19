package com.lifeos.auth.security;

import com.lifeos.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UserPrincipal principal) {
        return generateCustomToken(principal, accessTokenExpirationMs);
    }

    public String generateCustomToken(UserPrincipal principal, long expirationMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshTokenString() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("email", String.class);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
