package com.chatapp.internal.controller;

import com.chatapp.domain.model.Message;
import com.chatapp.domain.repository.MessageRepository;
import com.chatapp.internal.dto.CreateMessageRequest;
import com.chatapp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class InternalMessageController {

    private final MessageService messageService;
    private final MessageRepository messageRepository;

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

    /**
     * Polling için: verilen tarihten sonraki yeni mesajları getirir (ASC, en eski önce).
     * after: ISO-8601 Instant (URL-encoded kabul edilir)
     */
    @GetMapping("/new")
    public List<Message> getNewMessages(
            @PathVariable String roomId,
            @RequestParam String after) {
        return messageService.getNewMessages(roomId, after);
    }

    /**
     * Migration düzeltme: belirli bir mesajın createdAt'ini orijinal tarihle günceller.
     * Body: {"createdAt": "2024-03-15T10:30:00Z"}
     */
    @PatchMapping("/{messageId}/created-at")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fixCreatedAt(
            @PathVariable String roomId,
            @PathVariable String messageId,
            @RequestBody Map<String, String> body) {
        Instant original = Instant.parse(body.get("createdAt"));
        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setCreatedAt(original);
            messageRepository.save(msg);
        });
    }
}
