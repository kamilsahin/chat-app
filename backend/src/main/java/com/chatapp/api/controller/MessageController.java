package com.chatapp.api.controller;

import com.chatapp.api.dto.EditMessageRequest;
import com.chatapp.common.security.AuthHelper;
import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.User;
import com.chatapp.service.FcmNotificationService;
import com.chatapp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmNotificationService fcmNotificationService;

    @PostMapping("/api/rooms/{roomId}/messages/image")
    @ResponseStatus(HttpStatus.CREATED)
    public Message sendImage(
            @PathVariable String roomId,
            @RequestParam("file") MultipartFile file) throws IOException {
        User sender = AuthHelper.currentUser();
        Message message = messageService.sendImageMessage(roomId, sender.getExternalId(), file);
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);
        fcmNotificationService.notifyNewMessage(message, sender.getDisplayName());
        return message;
    }

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
        Message message = messageService.editMessage(messageId, request.content(), AuthHelper.currentUser().getExternalId());
        messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), message);
        return message;
    }

    @DeleteMapping("/api/messages/{messageId}")
    public Message deleteMessage(@PathVariable String messageId) {
        Message message = messageService.deleteMessage(messageId, AuthHelper.currentUser().getExternalId());
        messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), message);
        return message;
    }

    @PutMapping("/api/rooms/{roomId}/pin/{messageId}")
    public Message pinMessage(
            @PathVariable String roomId,
            @PathVariable String messageId) {
        return messageService.pinMessage(roomId, messageId, AuthHelper.currentUser().getExternalId());
    }

    @DeleteMapping("/api/rooms/{roomId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpinMessage(@PathVariable String roomId) {
        messageService.unpinMessage(roomId, AuthHelper.currentUser().getExternalId());
    }
}
