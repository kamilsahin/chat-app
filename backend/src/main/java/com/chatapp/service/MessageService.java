package com.chatapp.service;

import com.chatapp.config.ChatProperties;
import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.Room;
import com.chatapp.domain.model.Room.MemberRole;
import com.chatapp.domain.repository.MessageRepository;
import com.chatapp.domain.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final StorageService storageService;
    private final ChatProperties properties;

    public Message sendImageMessage(String roomId, String senderId, MultipartFile file) throws IOException {
        if (!properties.getFeatures().isFileSharingEnabled()) {
            throw new IllegalStateException("File sharing is disabled");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        String imageUrl = storageService.store(file);

        Message message = Message.builder()
                .roomId(roomId)
                .senderId(senderId)
                .type(Message.MessageType.IMAGE)
                .imageUrl(imageUrl)
                .build();

        return messageRepository.save(message);
    }

    public Slice<Message> getHistory(String roomId, String cursor) {
        PageRequest page = PageRequest.of(0, PAGE_SIZE);

        if (cursor == null) {
            return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, page);
        }

        Instant cursorInstant = Instant.parse(cursor);
        return messageRepository
                .findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursorInstant, page);
    }

    public Message editMessage(String messageId, String newContent, String requesterId) {
        Message message = findOrThrow(messageId);

        if (!message.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("Only the sender can edit this message");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        message.setContent(applyBlocklist(newContent));
        message.setEdited(true);
        message.setEditedAt(Instant.now());

        return messageRepository.save(message);
    }

    public Message deleteMessage(String messageId, String requesterId) {
        Message message = findOrThrow(messageId);

        boolean isSender = message.getSenderId().equals(requesterId);
        boolean isAdmin = isAdminInRoom(requesterId, message.getRoomId());
        boolean canDelete = switch (properties.getFeatures().getDeletePermission()) {
            case "ADMIN_ANY" -> isSender || isAdmin;
            default -> isSender; // OWN_ONLY
        };

        if (!canDelete) {
            throw new IllegalStateException("Not allowed to delete this message");
        }

        message.setDeleted(true);
        message.setContent("Bu mesaj silindi");
        return messageRepository.save(message);
    }

    public Message pinMessage(String roomId, String messageId, String requesterId) {
        if (!isAdminInRoom(requesterId, roomId)) {
            throw new IllegalStateException("Only admins can pin messages");
        }

        // Unpin current pinned message if any
        messageRepository.findByRoomIdAndIsPinnedTrue(roomId)
                .ifPresent(m -> {
                    m.setPinned(false);
                    messageRepository.save(m);
                });

        Message message = findOrThrow(messageId);
        message.setPinned(true);
        return messageRepository.save(message);
    }

    public void unpinMessage(String roomId, String requesterId) {
        if (!isAdminInRoom(requesterId, roomId)) {
            throw new IllegalStateException("Only admins can unpin messages");
        }

        messageRepository.findByRoomIdAndIsPinnedTrue(roomId)
                .ifPresent(m -> {
                    m.setPinned(false);
                    messageRepository.save(m);
                });
    }

    public Optional<Message> getPinnedMessage(String roomId) {
        return messageRepository.findByRoomIdAndIsPinnedTrue(roomId);
    }

    private Message findOrThrow(String messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
    }

    private boolean isAdminInRoom(String userId, String roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getMembers().stream()
                        .anyMatch(m -> m.getUserId().equals(userId)
                                && m.getRole() == MemberRole.ADMIN))
                .orElse(false);
    }

    private String applyBlocklist(String content) {
        String result = content;
        for (String keyword : properties.getFeatures().getKeywordBlocklist()) {
            result = result.replaceAll("(?i)" + keyword, "***");
        }
        return result;
    }
}
