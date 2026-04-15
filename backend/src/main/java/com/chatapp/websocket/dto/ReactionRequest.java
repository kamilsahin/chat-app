package com.chatapp.websocket.dto;

import jakarta.validation.constraints.NotBlank;

public record ReactionRequest(
        @NotBlank String messageId,
        @NotBlank String emoji
) {}
