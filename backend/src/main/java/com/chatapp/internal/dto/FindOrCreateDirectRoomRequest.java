package com.chatapp.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record FindOrCreateDirectRoomRequest(
        @NotBlank String userId1,
        @NotBlank String userId2
) {}
