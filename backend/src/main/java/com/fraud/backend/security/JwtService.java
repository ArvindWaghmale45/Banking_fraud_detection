package com.fraud.backend.security;

import com.fraud.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String encodedSecret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing key is not configured. Set JWT_SECRET to a Base64-encoded key of at least 32 bytes."
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(encodedSecret);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded key.", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes.");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("JWT expiration must be a positive number of seconds.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
