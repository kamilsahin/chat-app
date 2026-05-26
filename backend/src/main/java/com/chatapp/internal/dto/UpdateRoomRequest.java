package com.chatapp.internal.dto;

public record UpdateRoomRequest(
        String name,
        String avatarUrl
) {}
