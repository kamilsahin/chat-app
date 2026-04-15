package com.chatapp.websocket.dto;

public record TypingEvent(
        String userId,
        String displayName,
        boolean typing
) {}
