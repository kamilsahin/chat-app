package com.chatapp.domain.repository;

import com.chatapp.domain.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {

    // Cursor-based pagination: messages before a given cursor (createdAt), newest first
    Slice<Message> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            String roomId, Instant cursor, Pageable pageable);

    // First page (no cursor)
    Slice<Message> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    // New-messages polling: messages after a given timestamp, oldest first
    List<Message> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(String roomId, Instant after);

    // clearHistory: clearedAt filtreli ilk sayfa (cursor yok)
    Slice<Message> findByRoomIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String roomId, Instant clearedAt, Pageable pageable);

    // clearHistory: clearedAt filtreli cursor'lı sayfa
    Slice<Message> findByRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtBeforeOrderByCreatedAtDesc(
            String roomId, Instant clearedAt, Instant cursor, Pageable pageable);

    Optional<Message> findByRoomIdAndIsPinnedTrue(String roomId);

    Optional<Message> findFirstByRoomIdOrderByCreatedAtDesc(String roomId);

    @org.springframework.data.mongodb.repository.Query(
        value = "{ 'roomId': ?0, 'senderId': { $ne: ?1 }, 'readBy.userId': { $ne: ?1 }, 'isDeleted': false }",
        count = true
    )
    long countByRoomIdAndSenderIdNotAndReadByUserIdNot(String roomId, String userId);
}
