package com.myciti.security;

import com.myciti.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTtlMinutes;

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
        "myCitiSecretKeyForJwtSigningMustBeVeryLongAndSecure12345",
        "replace_with_at_least_32_random_bytes_for_jwt_signing",
        "your-jwt-secret-here-must-be-very-long-32-chars",
        "01234567890123456789012345678901",
        "12345678901234567890123456789012",
        "secretsecretsecretsecretsecret32c"
    );

    @Autowired
    public JwtProvider(
        @Value("${app.jwt.secret:}") String secret,
        @Value("${app.jwt.access-ttl-minutes:15}") long accessTtlMinutes
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured. Set the 'JWT_SECRET' environment variable with at least 32 characters.");
        }
        String trimmed = secret.trim();
        if (trimmed.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long (currently " + trimmed.length() + " chars).");
        }
        if (FORBIDDEN_SECRETS.contains(trimmed)) {
            throw new IllegalStateException("JWT_SECRET matches a known insecure default value. You must set a strong, random secret.");
        }

        this.key = Keys.hmacShaKeyFor(trimmed.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public JwtProvider(String secret, long accessTtlMinutes, String activeProfile) {
        this(secret, accessTtlMinutes);
    }

    public String generateToken(Long userId, String email, Role role, boolean profileComplete) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(accessTtlMinutes));

        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("email", email)
            .claim("role", role.name())
            .claim("profileComplete", profileComplete)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }

    /**
     * Parses and cryptographically validates the token in a single pass.
     * Returns the Claims payload if signature and expiry are valid, or null otherwise.
     */
    public Claims parseAndValidateClaims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception ex) {
            return null;
        }
    }

    public Claims getClaims(String token) {
        Claims claims = parseAndValidateClaims(token);
        if (claims == null) {
            throw new IllegalArgumentException("Invalid or expired JWT token");
        }
        return claims;
    }

    public boolean validateToken(String token) {
        return parseAndValidateClaims(token) != null;
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Boolean isProfileComplete(String token) {
        return getClaims(token).get("profileComplete", Boolean.class);
    }
}
