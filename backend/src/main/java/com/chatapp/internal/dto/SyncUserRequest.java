package com.chatapp.internal.dto;

public record SyncUserRequest(
        String displayName,
        String avatarUrl,
        String bio
) {}
