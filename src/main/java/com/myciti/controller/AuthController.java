package com.myciti.controller;

import com.myciti.dto.AuthResponse;
import com.myciti.dto.GoogleAuthRequest;
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
    private final boolean cookieSecure;

    public AuthController(
        AuthService authService,
        @Value("${app.auth.frontend-origin:http://localhost:5173}") String frontendOrigin,
        @Value("${app.auth.cookie-secure:false}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.frontendOrigin = frontendOrigin;
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
        return ResponseEntity.ok(new AuthResponse(result.accessToken(), result.user()));
    }

    /**
     * POST /api/auth/refresh
     * Checks Origin/Referer header to defend against CSRF, reads HttpOnly cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response,
        @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken
    ) {
        verifyOrigin(request);

        String token = cookieRefreshToken;
        if (token == null || token.isBlank()) {
            token = extractCookie(request, "refresh_token");
        }

        AuthService.AuthResult result = authService.refreshSession(token);
        setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(new AuthResponse(result.accessToken(), result.user()));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response,
        @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken
    ) {
        verifyOrigin(request);

        String token = cookieRefreshToken != null ? cookieRefreshToken : extractCookie(request, "refresh_token");
        authService.logout(token);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    private void verifyOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            if (frontendOrigin.equalsIgnoreCase(origin)) {
                return;
            }
            try {
                java.net.URI uri = java.net.URI.create(origin);
                String host = uri.getHost();
                if (host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"))) {
                    return;
                }
            } catch (Exception ignored) {
            }
            throw new org.springframework.security.access.AccessDeniedException("Invalid request origin");
        }
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/auth")
            .maxAge(30L * 24 * 60 * 60) // 30 days
            .sameSite("Lax")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/auth")
            .maxAge(0)
            .sameSite("Lax")
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
