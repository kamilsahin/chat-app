package com.chatapp.internal.dto;

import com.chatapp.domain.model.Room;

// lastMessage removed — Room.lastMessage (String, denormalized) is used instead.
public record RoomSummaryDto(
        Room room,
        long unreadCount
) {}
