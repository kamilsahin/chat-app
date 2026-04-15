package com.chatapp.internal.controller;

import com.chatapp.domain.model.Room;
import com.chatapp.internal.dto.AddMembersRequest;
import com.chatapp.internal.dto.CreateRoomRequest;
import com.chatapp.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
public class InternalRoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable String roomId) {
        roomService.deleteRoom(roomId);
    }

    @PostMapping("/{roomId}/members")
    public Room addMembers(
            @PathVariable String roomId,
            @Valid @RequestBody AddMembersRequest request) {
        return roomService.addMembers(roomId, request.memberIds());
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public Room removeMember(
            @PathVariable String roomId,
            @PathVariable String userId) {
        return roomService.removeMember(roomId, userId);
    }
}
