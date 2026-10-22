package com.myciti.security;

import com.myciti.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTtlMinutes;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtProvider.class);
    private static final String DEFAULT_SECRET = "myCitiSecretKeyForJwtSigningMustBeVeryLongAndSecure12345";

    public JwtProvider(
        @Value("${app.jwt.secret:" + DEFAULT_SECRET + "}") String secret,
        @Value("${app.jwt.access-ttl-minutes:15}") long accessTtlMinutes,
        @Value("${spring.profiles.active:default}") String activeProfile
    ) {
        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET;
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters long");
        }

        boolean isProduction = "prod".equalsIgnoreCase(activeProfile)
            || "production".equalsIgnoreCase(activeProfile)
            || "true".equalsIgnoreCase(System.getenv("RENDER"))
            || "production".equalsIgnoreCase(System.getenv("NODE_ENV"))
            || "production".equalsIgnoreCase(System.getenv("APP_ENV"));

        if (isProduction && DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("[CRITICAL SECURITY ERROR] Production deployment cannot use the default development JWT secret! Please set a strong JWT_SECRET environment variable.");
        }

        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("********************************************************************************");
            log.warn("[SECURITY WARNING] Application is using the default development JWT secret!");
            log.warn("To secure your deployment, add 'JWT_SECRET' to your Render Environment Variables.");
            log.warn("********************************************************************************");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public JwtProvider(String secret, long accessTtlMinutes) {
        this(secret, accessTtlMinutes, "default");
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
