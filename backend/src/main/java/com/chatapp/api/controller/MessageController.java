package com.chatapp.api.controller;

import com.chatapp.api.dto.EditMessageRequest;
import com.chatapp.common.security.AuthHelper;
import com.chatapp.domain.model.Message;
import com.chatapp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/api/rooms/{roomId}/messages")
    public Slice<Message> getHistory(
            @PathVariable String roomId,
            @RequestParam(required = false) String cursor) {
        return messageService.getHistory(roomId, cursor);
    }

    @GetMapping("/api/rooms/{roomId}/pin")
    public Message getPinnedMessage(@PathVariable String roomId) {
        return messageService.getPinnedMessage(roomId)
                .orElseThrow(() -> new IllegalArgumentException("No pinned message in room: " + roomId));
    }

    @PatchMapping("/api/messages/{messageId}")
    public Message editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return messageService.editMessage(messageId, request.content(), AuthHelper.currentUser().getId());
    }

    @DeleteMapping("/api/messages/{messageId}")
    public Message deleteMessage(@PathVariable String messageId) {
        return messageService.deleteMessage(messageId, AuthHelper.currentUser().getId());
    }

    @PutMapping("/api/rooms/{roomId}/pin/{messageId}")
    public Message pinMessage(
            @PathVariable String roomId,
            @PathVariable String messageId) {
        return messageService.pinMessage(roomId, messageId, AuthHelper.currentUser().getId());
    }

    @DeleteMapping("/api/rooms/{roomId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpinMessage(@PathVariable String roomId) {
        messageService.unpinMessage(roomId, AuthHelper.currentUser().getId());
    }
}
