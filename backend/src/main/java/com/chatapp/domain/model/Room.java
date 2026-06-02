package com.chatapp.domain.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Document(collection = "rooms")
public class Room {

    @Id
    private String id;

    private RoomType type;
    private String name;
    private String avatarUrl;

    @Builder.Default
    private List<Member> members = List.of();

    @Builder.Default
    private Boolean active = Boolean.TRUE;

    private Instant lastActivityAt;

    /** Denormalized preview of the last non-deleted message in this room. */
    private String lastMessage;

    @CreatedDate
    private Instant createdAt;

    /** Calculated at query time by RoomService; not meaningful when stored.
     *  Long (boxed) so old documents without this field deserialize as null → 0. */
    @Builder.Default
    private Long unreadCount = 0L;

    /** DIRECT odalarda karşı kullanıcının externalId'si. Query anında doldurulur. */
    private String otherUserId;

    // -------------------------------------------------------------------------

    public enum RoomType {
        DIRECT, GROUP
    }

    @Data
    @Builder
    public static class Member {
        private String userId;

        @Builder.Default
        private MemberRole role = MemberRole.MEMBER;

        private Instant joinedAt;

        @Builder.Default
        private boolean muted = false;

        private Instant mutedUntil;

        private Instant clearedAt;
    }

    public enum MemberRole {
        ADMIN, MEMBER
    }
}
