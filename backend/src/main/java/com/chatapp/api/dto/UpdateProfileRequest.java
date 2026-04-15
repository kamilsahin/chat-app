package com.chatapp.api.dto;

public record UpdateProfileRequest(
        String displayName,
        String avatarUrl,
        String bio
) {}
