package com.chatapp.service;

import com.chatapp.config.ChatProperties;
import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.Room.MemberRole;
import com.chatapp.domain.repository.MessageRepository;
import com.chatapp.domain.repository.RoomRepository;
import com.chatapp.internal.dto.CreateMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final StorageService storageService;
    private final ChatProperties properties;
    private final MongoTemplate mongoTemplate;

    public Message sendImageMessage(String roomId, String senderId, MultipartFile file, String caption) throws IOException {
        if (!properties.getFeatures().isFileSharingEnabled()) {
            throw new IllegalStateException("File sharing is disabled");
        }
        String contentType = file.getContentType();
        log.info("Image upload: contentType={}, originalFilename={}, size={}", contentType, file.getOriginalFilename(), file.getSize());
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed, received: " + contentType);
        }

        String imageUrl = storageService.store(file);
        String safeCaption = (caption != null && !caption.isBlank()) ? applyBlocklist(caption.trim()) : null;

        Message message = Message.builder()
                .roomId(roomId)
                .senderId(senderId)
                .type(Message.MessageType.IMAGE)
                .imageUrl(imageUrl)
                .content(safeCaption)
                .createdAt(Instant.now())
                .build();

        Message saved = messageRepository.save(message);
        touchRoom(roomId, saved);
        return saved;
    }

    public Message createMessage(String roomId, CreateMessageRequest request) {
        Message message = Message.builder()
                .roomId(roomId)
                .senderId(request.senderId())
                .type(request.type())
                .content(request.content() != null ? applyBlocklist(request.content()) : null)
                .replyTo(request.replyTo())
                .createdAt(request.createdAt() != null ? request.createdAt() : Instant.now())
                .build();
        Message saved = messageRepository.save(message);
        touchRoom(roomId, saved);
        return saved;
    }

    private void touchRoom(String roomId, Message message) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setLastActivityAt(message.getCreatedAt());
            if (!message.isDeleted()) {
                String preview = switch (message.getType()) {
                    case IMAGE    -> "📷 Fotoğraf";
                    case LOCATION -> "📍 Konum";
                    default       -> message.getContent();
                };
                room.setLastMessage(preview);
            }
            roomRepository.save(room);
        });
    }

    public Slice<Message> getHistory(String roomId, String cursor) {
        PageRequest page = PageRequest.of(0, PAGE_SIZE);

        if (cursor == null) {
            return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, page);
        }

        // Cursor may arrive URL-encoded (e.g. %3A for :) from RestTemplate callers
        String decoded = java.net.URLDecoder.decode(cursor, java.nio.charset.StandardCharsets.UTF_8);
        Instant cursorInstant = Instant.parse(decoded);
        return messageRepository
                .findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursorInstant, page);
    }

    public List<Message> getNewMessages(String roomId, String after) {
        String decoded = java.net.URLDecoder.decode(after, java.nio.charset.StandardCharsets.UTF_8);
        Instant afterInstant = Instant.parse(decoded);
        return messageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, afterInstant);
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

    /**
     * Bulk-marks all unread messages in a room as read for the given user.
     * Called when the user opens a room so the next room-list fetch shows 0 unread.
     */
    public void markAllAsRead(String roomId, String userId) {
        Query q = Query.query(
                Criteria.where("roomId").is(roomId)
                        .and("senderId").ne(userId)
                        .and("readBy.userId").ne(userId)
                        .and("isDeleted").ne(true)
        );
        var receipt = Message.ReadReceipt.builder()
                .userId(userId)
                .readAt(Instant.now())
                .build();
        mongoTemplate.updateMulti(q, new Update().push("readBy", receipt), Message.class);
    }

    private String applyBlocklist(String content) {
        String result = content;
        for (String keyword : properties.getFeatures().getKeywordBlocklist()) {
            result = result.replaceAll("(?i)" + keyword, "***");
        }
        return result;
    }
}
