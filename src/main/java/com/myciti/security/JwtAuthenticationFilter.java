package com.myciti.security;

import com.myciti.event.UserStatusChangedEvent;
import com.myciti.model.Role;
import com.myciti.model.User;
import com.myciti.model.UserStatus;
import com.myciti.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    private static final long CACHE_TTL_MS = 60_000L; // 60 seconds TTL

    private static class CachedUser {
        final User user;
        final long cachedAt;

        CachedUser(User user) {
            this.user = user;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - cachedAt) > CACHE_TTL_MS;
        }
    }

    private final java.util.Map<Long, CachedUser> userCache = new java.util.concurrent.ConcurrentHashMap<>();

    public void evictUser(Long userId) {
        if (userId != null) {
            userCache.remove(userId);
        }
    }

    @EventListener
    public void onUserStatusChanged(UserStatusChangedEvent event) {
        if (event != null && event.userId() != null) {
            evictUser(event.userId());
            log.debug("[JwtAuthenticationFilter] Evicted user #{} from security cache on status change", event.userId());
        }
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            Claims claims = jwtProvider.parseAndValidateClaims(token);
            if (claims != null) {
                try {
                    Long userId = Long.parseLong(claims.getSubject());

                    if (userId != null) {
                    User user = null;
                    CachedUser cached = userCache.get(userId);
                    if (cached != null && !cached.isExpired()) {
                        user = cached.user;
                    } else {
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isPresent()) {
                            user = userOpt.get();
                            if (userCache.size() > 5000) {
                                userCache.entrySet().removeIf(e -> e.getValue().isExpired());
                            }
                            userCache.put(userId, new CachedUser(user));
                        }
                    }

                    if (user != null) {
                        if (user.getStatus() == UserStatus.ACTIVE) {
                            String role = "ROLE_" + user.getRole().name();
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            userCache.remove(userId);
                            log.warn("[JwtAuthenticationFilter] Inactive or suspended user #{} attempted request to {}", userId, request.getRequestURI());
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("[JwtAuthenticationFilter] Error parsing JWT or loading user context: {}", ex.getMessage());
            }
        }
    }

    filterChain.doFilter(request, response);
}

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
