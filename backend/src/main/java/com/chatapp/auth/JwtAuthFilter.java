package com.chatapp.auth;

import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.UserRepository;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        jwtService.parse(token).ifPresent(payload -> {
            User user = resolveUser(payload);
            var authentication = new UsernamePasswordAuthenticationToken(
                    user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });

        filterChain.doFilter(request, response);
    }

    // Auto-create or update user profile on every authenticated request.
    private User resolveUser(Map<String, Object> payload) {
        Object idObj = payload.get("id");
        Object subObj = payload.get("sub");
        String externalId = idObj != null ? String.valueOf(idObj)
                          : subObj != null ? String.valueOf(subObj) : null;
        String displayName = payload.get("displayName") != null
                ? String.valueOf(payload.get("displayName")) : null;
        String avatarUrl = payload.get("avatarUrl") != null
                ? String.valueOf(payload.get("avatarUrl")) : null;

        return userRepository.findByExternalId(externalId)
                .map(existing -> {
                    boolean dirty = false;
                    if (displayName != null && !displayName.equals(existing.getDisplayName())) {
                        existing.setDisplayName(displayName);
                        dirty = true;
                    }
                    if (avatarUrl != null && !avatarUrl.equals(existing.getAvatarUrl())) {
                        existing.setAvatarUrl(avatarUrl);
                        dirty = true;
                    }
                    return dirty ? userRepository.save(existing) : existing;
                })
                .orElseGet(() -> {
                    log.info("Auto-creating user profile for externalId={}", externalId);
                    User newUser = User.builder()
                            .externalId(externalId)
                            .displayName(displayName != null ? displayName : externalId)
                            .avatarUrl(avatarUrl)
                            .createdAt(Instant.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
