package com.chatapp.service;

import com.chatapp.config.ChatProperties;
import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.Message.Location;
import com.chatapp.domain.model.Message.Reaction;
import com.chatapp.domain.model.Message.ReadReceipt;
import com.chatapp.domain.repository.MessageRepository;
import com.chatapp.websocket.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatProperties properties;

    public Message saveMessage(String roomId, String senderId, SendMessageRequest request) {
        String content = request.content() != null
                ? applyBlocklist(request.content())
                : null;

        Location location = null;
        if (request.location() != null) {
            location = Location.builder()
                    .latitude(request.location().latitude())
                    .longitude(request.location().longitude())
                    .address(request.location().address())
                    .build();
        }

        Message message = Message.builder()
                .roomId(roomId)
                .senderId(senderId)
                .type(request.type())
                .content(content)
                .replyTo(request.replyTo())
                .forwardedFrom(request.forwardedFrom())
                .imageUrl(request.imageUrl())
                .location(location)
                .build();

        return messageRepository.save(message);
    }

    public Message toggleReaction(String messageId, String userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        List<Reaction> reactions = new ArrayList<>(message.getReactions());

        Reaction existing = reactions.stream()
                .filter(r -> r.getEmoji().equals(emoji))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            // New emoji reaction
            List<String> userIds = new ArrayList<>();
            userIds.add(userId);
            reactions.add(Reaction.builder().emoji(emoji).userIds(userIds).build());
        } else {
            List<String> userIds = new ArrayList<>(existing.getUserIds());
            if (userIds.contains(userId)) {
                // Remove reaction
                userIds.remove(userId);
                if (userIds.isEmpty()) {
                    reactions.remove(existing);
                } else {
                    existing.setUserIds(userIds);
                }
            } else {
                // Add reaction
                userIds.add(userId);
                existing.setUserIds(userIds);
            }
        }

        message.setReactions(reactions);
        return messageRepository.save(message);
    }

    public Message markAsRead(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        boolean alreadyRead = message.getReadBy().stream()
                .anyMatch(r -> r.getUserId().equals(userId));

        if (!alreadyRead) {
            List<ReadReceipt> readBy = new ArrayList<>(message.getReadBy());
            readBy.add(ReadReceipt.builder().userId(userId).readAt(Instant.now()).build());
            message.setReadBy(readBy);
            return messageRepository.save(message);
        }

        return message;
    }

    private String applyBlocklist(String content) {
        String result = content;
        for (String keyword : properties.getFeatures().getKeywordBlocklist()) {
            result = result.replaceAll("(?i)" + keyword, "***");
        }
        return result;
    }
}
