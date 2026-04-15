package com.chatapp.websocket.dto;

import java.time.Instant;

public record PresenceEvent(
        String userId,
        boolean online,
        Instant lastSeen
) {}
