package com.chatapp.domain.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "room_created", def = "{'roomId': 1, 'createdAt': -1}")
})
public class Message {

    @Id
    private String id;

    private String roomId;
    private String senderId;

    private MessageType type;
    private String content;

    private String replyTo;
    private String forwardedFrom;

    @Builder.Default
    private List<Reaction> reactions = List.of();

    @Builder.Default
    private List<ReadReceipt> readBy = List.of();

    @Builder.Default
    private boolean isDeleted = false;

    @Builder.Default
    private boolean isEdited = false;

    private Instant editedAt;

    private String imageUrl;
    private Location location;

    @Builder.Default
    private boolean isPinned = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // -------------------------------------------------------------------------

    public enum MessageType {
        TEXT, IMAGE, LOCATION
    }

    @Data
    @Builder
    public static class Reaction {
        private String emoji;

        @Builder.Default
        private List<String> userIds = List.of();
    }

    @Data
    @Builder
    public static class ReadReceipt {
        private String userId;
        private Instant readAt;
    }

    @Data
    @Builder
    public static class Location {
        private double latitude;
        private double longitude;
        private String address;
    }
}
