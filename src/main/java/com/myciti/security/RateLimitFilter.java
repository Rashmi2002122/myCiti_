package com.myciti.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myciti.dto.ErrorResponse;
import com.myciti.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            if (!rateLimitingService.tryConsumeAuth(clientIp)) {
                sendRateLimitResponse(response, path, "Rate limit exceeded for authentication requests (max 20 per minute)");
                return;
            }
        } else if (path.startsWith("/api/ads/campaigns/") && (path.endsWith("/impressions") || path.endsWith("/click"))) {
            String clientIp = getClientIp(request);
            if (!rateLimitingService.tryConsumeAdAction(clientIp)) {
                sendRateLimitResponse(response, path, "Rate limit exceeded for ad interaction tracking (max 60 per minute)");
                return;
            }
        } else if ("POST".equalsIgnoreCase(method) && (path.endsWith("/images") || path.endsWith("/logo") || path.endsWith("/cover"))) {
            String clientIp = getClientIp(request);
            if (!rateLimitingService.tryConsumeImageUpload(clientIp)) {
                sendRateLimitResponse(response, path, "Rate limit exceeded for image uploads (max 15 per minute)");
                return;
            }
        } else if ("GET".equalsIgnoreCase(method) && (path.equals("/api/listings") || path.equals("/api/listings/"))) {
            String clientIp = getClientIp(request);
            if (!rateLimitingService.tryConsumeSearch(clientIp)) {
                sendRateLimitResponse(response, path, "Rate limit exceeded for listing queries (max 60 per minute)");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Too Many Requests",
            message,
            path
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String getClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        // Only trust X-Forwarded-For if request is coming from localhost/reverse proxy
        if (isLocalOrProxy(remoteAddr)) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) {
                String[] parts = xf.split(",");
                // Scan right-to-left to find the real edge client IP added by the reverse proxy
                for (int i = parts.length - 1; i >= 0; i--) {
                    String candidate = parts[i].trim();
                    if (!candidate.isEmpty() && !isLocalOrProxy(candidate)) {
                        return candidate;
                    }
                }
                return parts[0].trim();
            }
        }
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }

    private boolean isLocalOrProxy(String ip) {
        if (ip == null) return false;
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            try {
                String[] parts = ip.split("\\.");
                if (parts.length >= 2) {
                    int secondOctet = Integer.parseInt(parts[1]);
                    return secondOctet >= 16 && secondOctet <= 31;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
