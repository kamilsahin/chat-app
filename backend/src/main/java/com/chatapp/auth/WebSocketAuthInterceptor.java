package com.chatapp.auth;

import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rejected: missing Authorization header");
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        Claims claims = jwtService.parse(token)
                .orElseThrow(() -> {
                    log.warn("WebSocket CONNECT rejected: invalid JWT");
                    return new IllegalArgumentException("Invalid JWT");
                });

        User user = resolveUser(claims);

        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        return message;
    }

    private User resolveUser(Claims claims) {
        String externalId = claims.getSubject();
        String displayName = claims.get("displayName", String.class);

        return userRepository.findByExternalId(externalId)
                .orElseGet(() -> {
                    log.info("Auto-creating user for WebSocket connect, externalId={}", externalId);
                    return userRepository.save(User.builder()
                            .externalId(externalId)
                            .displayName(displayName != null ? displayName : externalId)
                            .createdAt(Instant.now())
                            .build());
                });
    }
}
