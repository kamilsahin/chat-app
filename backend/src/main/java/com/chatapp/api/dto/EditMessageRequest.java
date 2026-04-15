package com.chatapp.api.dto;

import jakarta.validation.constraints.NotBlank;

public record EditMessageRequest(
        @NotBlank String content
) {}
