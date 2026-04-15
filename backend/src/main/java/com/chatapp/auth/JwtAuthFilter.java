package com.chatapp.auth;

import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.UserRepository;
import io.jsonwebtoken.Claims;
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

        jwtService.parse(token).ifPresent(claims -> {
            User user = resolveUser(claims);
            var authentication = new UsernamePasswordAuthenticationToken(
                    user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });

        filterChain.doFilter(request, response);
    }

    // On first valid connection, auto-create a local user profile if it doesn't exist.
    private User resolveUser(Claims claims) {
        String externalId = claims.getSubject();
        String displayName = claims.get("displayName", String.class);

        return userRepository.findByExternalId(externalId)
                .orElseGet(() -> {
                    log.info("Auto-creating user profile for externalId={}", externalId);
                    User newUser = User.builder()
                            .externalId(externalId)
                            .displayName(displayName != null ? displayName : externalId)
                            .createdAt(Instant.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
