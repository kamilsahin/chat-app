package com.chatapp.websocket.dto;

import com.chatapp.domain.model.Message.MessageType;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotNull MessageType type,
        String content,
        String replyTo,
        String forwardedFrom,
        String imageUrl,
        LocationPayload location
) {
    public record LocationPayload(double latitude, double longitude, String address) {}
}
