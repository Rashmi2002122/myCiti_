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
    private static final String DEFAULT_SECRET = "insecure_default_secret_placeholder_must_change_32chars";

    public JwtProvider(
        @Value("${app.jwt.secret:" + DEFAULT_SECRET + "}") String secret,
        @Value("${app.jwt.access-ttl-minutes:15}") long accessTtlMinutes
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters long");
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

    public Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
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
