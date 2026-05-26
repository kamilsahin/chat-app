package com.chatapp.internal.dto;

import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.Room;

public record RoomSummaryDto(
        Room room,
        Message lastMessage,
        long unreadCount
) {}
