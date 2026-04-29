package com.chatapp.internal.controller;

import com.chatapp.domain.model.Message;
import com.chatapp.internal.dto.CreateMessageRequest;
import com.chatapp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class InternalMessageController {

    private final MessageService messageService;

    /**
     * Migration ve server-side mesaj yazma için.
     * createdAt verilirse eski tarih korunur (migration), verilmezse şimdiki zaman kullanılır.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Message createMessage(
            @PathVariable String roomId,
            @Valid @RequestBody CreateMessageRequest request) {
        return messageService.createMessage(roomId, request);
    }

    /**
     * Oda mesajlarını sayfalı getirir.
     * cursor: ISO-8601 Instant — bu tarihten önceki mesajları getirir (scroll up / load more)
     */
    @GetMapping
    public Slice<Message> getMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) String cursor) {
        return messageService.getHistory(roomId, cursor);
    }
}
