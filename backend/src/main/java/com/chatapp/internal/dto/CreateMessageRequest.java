package com.chatapp.internal.dto;

import com.chatapp.domain.model.Message.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateMessageRequest(
        @NotBlank String senderId,
        @NotNull MessageType type,
        String content,
        String replyTo,
        Instant createdAt   // migration için — null ise şimdiki zaman kullanılır
) {}
