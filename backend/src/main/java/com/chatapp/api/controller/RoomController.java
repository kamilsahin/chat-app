package com.chatapp.api.controller;

import com.chatapp.api.dto.MuteRequest;
import com.chatapp.common.security.AuthHelper;
import com.chatapp.domain.model.Room;
import com.chatapp.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<Room> listRooms(
            @RequestParam(required = false) Room.RoomType type) {
        return roomService.getRoomsForUser(AuthHelper.currentUser().getExternalId(), type);
    }

    @GetMapping("/{roomId}")
    public Room getRoom(@PathVariable String roomId) {
        return roomService.getRoom(roomId);
    }

    @PutMapping("/{roomId}/mute")
    public Room muteRoom(
            @PathVariable String roomId,
            @Valid @RequestBody MuteRequest request) {
        return roomService.muteRoom(roomId, AuthHelper.currentUser().getExternalId(), request.duration());
    }

    @DeleteMapping("/{roomId}/mute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmuteRoom(@PathVariable String roomId) {
        roomService.unmuteRoom(roomId, AuthHelper.currentUser().getExternalId());
    }
}
