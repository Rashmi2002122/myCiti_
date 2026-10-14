package com.myciti.controller;

import com.myciti.dto.AuthResponse;
import com.myciti.dto.GoogleAuthRequest;
import com.myciti.dto.RefreshTokenRequest;
import com.myciti.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String frontendOrigin;
    private final String allowedOrigins;
    private final boolean cookieSecure;

    public AuthController(
        AuthService authService,
        @Value("${app.auth.frontend-origin:http://localhost:5173}") String frontendOrigin,
        @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String allowedOrigins,
        @Value("${app.auth.cookie-secure:false}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.frontendOrigin = frontendOrigin;
        this.allowedOrigins = allowedOrigins;
        this.cookieSecure = cookieSecure;
    }

    /**
     * POST /api/auth/google
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
        @Valid @RequestBody GoogleAuthRequest request,
        HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.loginWithGoogle(request.getIdToken());
        setRefreshTokenCookie(response, result.rawRefreshToken());
        // Return both accessToken and refreshToken to support cross-domain SPAs where 3rd-party cookies might be blocked
        return ResponseEntity.ok(new AuthResponse(result.accessToken(), result.rawRefreshToken(), result.user()));
    }

    /**
     * POST /api/auth/refresh
     * Reads refresh token from:
     * 1. HttpOnly Cookie (for same-origin or modern browsers permitting partitioned cookies)
     * 2. X-Refresh-Token request header
     * 3. Optional JSON request body
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response,
        @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
        @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken,
        @RequestBody(required = false) RefreshTokenRequest bodyRequest
    ) {
        verifyOrigin(request);

        String token = cookieRefreshToken;
        if (token == null || token.isBlank()) {
            token = headerRefreshToken;
        }
        if (token == null || token.isBlank()) {
            if (bodyRequest != null && bodyRequest.getRefreshToken() != null) {
                token = bodyRequest.getRefreshToken();
            }
        }
        if (token == null || token.isBlank()) {
            token = extractCookie(request, "refresh_token");
        }

        AuthService.AuthResult result = authService.refreshSession(token);
        setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(new AuthResponse(result.accessToken(), result.rawRefreshToken(), result.user()));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response,
        @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
        @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken,
        @RequestBody(required = false) RefreshTokenRequest bodyRequest
    ) {
        verifyOrigin(request);

        String token = cookieRefreshToken;
        if (token == null || token.isBlank()) {
            token = headerRefreshToken;
        }
        if (token == null || token.isBlank()) {
            if (bodyRequest != null && bodyRequest.getRefreshToken() != null) {
                token = bodyRequest.getRefreshToken();
            }
        }
        if (token == null || token.isBlank()) {
            token = extractCookie(request, "refresh_token");
        }

        authService.logout(token);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    private void verifyOrigin(HttpServletRequest request) {
        // If request provides token explicitly via X-Refresh-Token header, CSRF is not applicable
        if (request.getHeader("X-Refresh-Token") != null && !request.getHeader("X-Refresh-Token").isBlank()) {
            return;
        }

        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return;
        }

        String normalizedOrigin = origin.trim().replaceAll("/+$", "");
        String normalizedFrontend = frontendOrigin.trim().replaceAll("/+$", "");

        if (normalizedOrigin.equalsIgnoreCase(normalizedFrontend)) {
            return;
        }

        if (allowedOrigins != null) {
            for (String allowed : allowedOrigins.split(",")) {
                String normalizedAllowed = allowed.trim().replaceAll("/+$", "");
                if (normalizedAllowed.equalsIgnoreCase(normalizedOrigin) || "*".equals(normalizedAllowed)) {
                    return;
                }
            }
        }

        try {
            java.net.URI uri = java.net.URI.create(origin);
            String host = uri.getHost();
            if (host != null && (
                host.equalsIgnoreCase("localhost") ||
                host.equals("127.0.0.1") ||
                host.endsWith(".vercel.app") ||
                host.endsWith(".onrender.com") ||
                host.endsWith(".pages.dev")
            )) {
                return;
            }
        } catch (Exception ignored) {
        }
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // When cookieSecure is true (production HTTPS cross-domain), use SameSite=None so browser permits cross-origin cookies.
        String sameSitePolicy = cookieSecure ? "None" : "Lax";
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/auth")
            .maxAge(30L * 24 * 60 * 60) // 30 days
            .sameSite(sameSitePolicy);

        ResponseCookie cookie = builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        String sameSitePolicy = cookieSecure ? "None" : "Lax";
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/auth")
            .maxAge(0)
            .sameSite(sameSitePolicy)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
