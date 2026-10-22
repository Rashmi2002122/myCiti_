package com.myciti.security;

import com.myciti.model.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtProviderTest {

    private static final String VALID_SECRET = "a-very-strong-and-secure-random-secret-key-32-bytes!";

    @Test
    @DisplayName("Should reject null, empty or blank secret on startup")
    void shouldRejectNullOrBlankSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtProvider(null, 15));
        assertThrows(IllegalStateException.class, () -> new JwtProvider("", 15));
        assertThrows(IllegalStateException.class, () -> new JwtProvider("   ", 15));
    }

    @Test
    @DisplayName("Should reject secret shorter than 32 characters")
    void shouldRejectShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtProvider("short-secret-key", 15));
    }

    @Test
    @DisplayName("Should reject known default insecure development secrets")
    void shouldRejectKnownInsecureDefaults() {
        assertThrows(IllegalStateException.class, () -> 
            new JwtProvider("myCitiSecretKeyForJwtSigningMustBeVeryLongAndSecure12345", 15));
        assertThrows(IllegalStateException.class, () -> 
            new JwtProvider("replace_with_at_least_32_random_bytes_for_jwt_signing", 15));
        assertThrows(IllegalStateException.class, () -> 
            new JwtProvider("01234567890123456789012345678901", 15));
    }

    @Test
    @DisplayName("Should initialize cleanly with valid secret and generate/validate token")
    void shouldInitializeAndSignValidToken() {
        JwtProvider provider = new JwtProvider(VALID_SECRET, 15);
        String token = provider.generateToken(42L, "test@example.com", Role.USER, true);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = provider.parseAndValidateClaims(token);
        assertNotNull(claims);
        assertEquals("42", claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("USER", claims.get("role", String.class));
        assertEquals(Boolean.TRUE, claims.get("profileComplete", Boolean.class));

        assertEquals(42L, provider.getUserId(token));
        assertEquals("test@example.com", provider.getEmail(token));
        assertEquals("USER", provider.getRole(token));
        assertTrue(provider.isProfileComplete(token));
    }

    @Test
    @DisplayName("Should return null for tampered token")
    void shouldRejectTamperedToken() {
        JwtProvider provider = new JwtProvider(VALID_SECRET, 15);
        String token = provider.generateToken(42L, "test@example.com", Role.USER, true);

        String tampered = token.substring(0, token.length() - 5) + "abcde";
        assertNull(provider.parseAndValidateClaims(tampered));
        assertFalse(provider.validateToken(tampered));
    }
}
