package org.example.flashsale.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.flashsale.model.Role;
import org.example.flashsale.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed token for a user.
     * Includes the user's role as a claim.
     */
    public String generate(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())   // ← role in the token
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Extract the user id from a valid token.
     */
    public Optional<Long> extractUserId(String token) {
        return extractClaims(token).map(claims -> Long.parseLong(claims.getSubject()));
    }

    /**
     * Extract the role from a valid token.
     */
    public Optional<Role> extractRole(String token) {
        return extractClaims(token).map(claims -> {
            String role = claims.get("role", String.class);
            return role == null ? Role.BUYER : Role.valueOf(role);
        });
    }

    /**
     * Extract the email from a valid token.
     */
    public Optional<String> extractEmail(String token) {
        return extractClaims(token).map(claims -> claims.get("email", String.class));
    }

    private Optional<Claims> extractClaims(String token) {
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