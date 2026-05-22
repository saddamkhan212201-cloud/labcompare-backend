package com.labcompare.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:labcompare-super-secret-key-minimum-256-bits-long-for-hs256}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String role, Long adminLabId, String phone) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("adminLabId", adminLabId)
                .claim("phone", phone)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    /** Overload for callers that don't yet pass phone (backward compat) */
    public String generateToken(String username, String role, Long adminLabId) {
        return generateToken(username, role, adminLabId, null);
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long extractAdminLabId(String token) {
        Object val = getClaims(token).get("adminLabId");
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return null;
    }

    public String extractPhone(String token) {
        return getClaims(token).get("phone", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
