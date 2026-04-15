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

    @CreatedDate
    private Instant createdAt;

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
    }

    public enum MemberRole {
        ADMIN, MEMBER
    }
}
