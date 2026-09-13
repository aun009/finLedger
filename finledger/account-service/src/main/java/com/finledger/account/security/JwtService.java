package com.finledger.account.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration tokenLifetime;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiration:900s}") Duration tokenLifetime) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenLifetime = tokenLifetime;
    }

    public String issue(String subject, String role) {
        Instant now = Instant.now();
        return Jwts.builder().subject(subject).claim("role", role)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(tokenLifetime)))
                .signWith(signingKey).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }

    public long expiresInSeconds() { return tokenLifetime.toSeconds(); }
}
