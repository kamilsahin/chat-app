package com.chatapp.internal.dto;

public record SyncUserRequest(
        String displayName,
        String nickname,
        String avatarUrl,
        String bio
) {}
