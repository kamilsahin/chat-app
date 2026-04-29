package com.chatapp.api.dto;

public record UpdateProfileRequest(
        String displayName,
        String nickname,
        String avatarUrl,
        String bio
) {}
