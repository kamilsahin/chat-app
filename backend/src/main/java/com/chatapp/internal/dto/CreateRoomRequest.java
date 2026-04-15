package com.chatapp.internal.dto;

import com.chatapp.domain.model.Room.RoomType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRoomRequest(
        @NotNull RoomType type,
        String name,
        String avatarUrl,
        @NotEmpty List<String> memberIds,
        String adminId
) {}
