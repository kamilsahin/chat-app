package com.chatapp.websocket.controller;

import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.User;
import com.chatapp.service.ChatService;
import com.chatapp.service.FcmNotificationService;
import com.chatapp.websocket.dto.ReactionRequest;
import com.chatapp.websocket.dto.SendMessageRequest;
import com.chatapp.websocket.dto.TypingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmNotificationService fcmNotificationService;

    // Client publishes to: /app/room.{roomId}.send
    // Server broadcasts to: /topic/room.{roomId}
    @MessageMapping("/room.{roomId}.send")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload SendMessageRequest request,
            Principal principal) {

        User user = getUser(principal);
        Message message = chatService.saveMessage(roomId, user.getExternalId(), request);
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);
        fcmNotificationService.notifyNewMessage(message, user.getDisplayName());
    }

    // Client publishes to: /app/room.{roomId}.typing
    // Server broadcasts to: /topic/room.{roomId}.typing
    @MessageMapping("/room.{roomId}.typing")
    public void typing(
            @DestinationVariable String roomId,
            @Payload TypingEvent request,
            Principal principal) {

        User user = getUser(principal);
        TypingEvent event = new TypingEvent(user.getExternalId(), user.getDisplayName(), request.typing());
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", event);
    }

    // Client publishes to: /app/room.{roomId}.reaction
    // Server broadcasts updated message to: /topic/room.{roomId}
    @MessageMapping("/room.{roomId}.reaction")
    public void reaction(
            @DestinationVariable String roomId,
            @Payload ReactionRequest request,
            Principal principal) {

        User user = getUser(principal);
        Message message = chatService.toggleReaction(request.messageId(), user.getExternalId(), request.emoji());
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);
    }

    // Client publishes to: /app/room.{roomId}.read
    // Server broadcasts updated message to: /topic/room.{roomId}
    @MessageMapping("/room.{roomId}.read")
    public void markRead(
            @DestinationVariable String roomId,
            @Payload String messageId,
            Principal principal) {

        User user = getUser(principal);
        Message message = chatService.markAsRead(messageId, user.getExternalId());
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);
    }

    private User getUser(Principal principal) {
        return (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
    }
}
