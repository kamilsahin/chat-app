package com.chatapp.api.controller;

import com.chatapp.api.dto.MuteRequest;
import com.chatapp.common.security.AuthHelper;
import com.chatapp.domain.model.Room;
import com.chatapp.service.MessageService;
import com.chatapp.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MessageService messageService;

    @GetMapping
    public Slice<Room> listRooms(
            @RequestParam(required = false) Room.RoomType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomService.getRoomsForUser(AuthHelper.currentUser().getExternalId(), type, page, size);
    }

    @GetMapping("/{roomId}")
    public Room getRoom(@PathVariable String roomId) {
        return roomService.getRoomEnriched(roomId, AuthHelper.currentUser().getExternalId());
    }

    /**
     * Karşı kullanıcıyla DIRECT odayı bulur/oluşturur ve enrich edilmiş döner.
     * Profilden "Mesaj yaz" akışı bunu çağırır.
     */
    @PostMapping("/direct")
    public Room createDirectRoom(@RequestBody java.util.Map<String, String> body) {
        String otherUserId = body.get("userId");
        return roomService.findOrCreateDirectRoomEnriched(
                AuthHelper.currentUser().getExternalId(), otherUserId);
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

    /** Mark all messages in this room as read for the current user. */
    @PostMapping("/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRoomAsRead(@PathVariable String roomId) {
        messageService.markAllAsRead(roomId, AuthHelper.currentUser().getExternalId());
    }

    /**
     * Sohbet geçmişini sadece bu kullanıcı için temizler.
     * Mesajlar sistemden silinmez; diğer üyeler görmeye devam eder.
     */
    @DeleteMapping("/{roomId}/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory(@PathVariable String roomId) {
        roomService.clearHistory(roomId, AuthHelper.currentUser().getExternalId());
    }
}
