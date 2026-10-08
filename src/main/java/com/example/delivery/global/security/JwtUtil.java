package com.example.delivery.global.security;

import com.example.delivery.user.entity.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_KEY = "role";

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String createToken(String username, UserRole role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(username)
                .claim(ROLE_KEY, role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어 있음: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT 형식이 올바르지 않음: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT (서명 없음 등): {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 서명이 일치하지 않음: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(parseClaims(token).get(ROLE_KEY, String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}