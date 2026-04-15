package com.chatapp.auth;

import com.chatapp.config.ChatProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalApiFilter extends OncePerRequestFilter {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final ChatProperties properties;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String secret = request.getHeader(INTERNAL_SECRET_HEADER);
        String expected = properties.getInternal().getApiSecret();

        if (expected == null || !expected.equals(secret)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }
}
