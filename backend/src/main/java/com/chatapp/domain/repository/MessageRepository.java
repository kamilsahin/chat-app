package com.chatapp.domain.repository;

import com.chatapp.domain.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {

    // Cursor-based pagination: messages before a given cursor (createdAt), newest first
    Slice<Message> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            String roomId, Instant cursor, Pageable pageable);

    // First page (no cursor)
    Slice<Message> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    Optional<Message> findByRoomIdAndIsPinnedTrue(String roomId);
}
