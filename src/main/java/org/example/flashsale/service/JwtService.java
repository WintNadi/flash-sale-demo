package org.example.flashsale.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.example.flashsale.model.Role;
import org.example.flashsale.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        log.info("JwtService initializing. Secret length = {} chars, expiration = {} ms",
                secret == null ? "null" : secret.length(), expirationMs);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Optional<Long> extractUserId(String token) {
        return extractClaims(token).map(claims -> Long.parseLong(claims.getSubject()));
    }

    public Optional<Role> extractRole(String token) {
        return extractClaims(token).map(claims -> {
            String role = claims.get("role", String.class);
            return role == null ? Role.BUYER : Role.valueOf(role);
        });
    }

    public Optional<String> extractEmail(String token) {
        return extractClaims(token).map(claims -> claims.get("email", String.class));
    }

    /** Public for debug purposes. */
    public Optional<Claims> extractClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}