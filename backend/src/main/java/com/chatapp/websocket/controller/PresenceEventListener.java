package com.chatapp.websocket.controller;

import com.chatapp.domain.model.User;
import com.chatapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        extractUserId(event.getUser()).ifPresent(presenceService::onConnect);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        extractUserId(event.getUser()).ifPresent(presenceService::onDisconnect);
    }

    private java.util.Optional<String> extractUserId(java.security.Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            return java.util.Optional.of(user.getId());
        }
        return java.util.Optional.empty();
    }
}
