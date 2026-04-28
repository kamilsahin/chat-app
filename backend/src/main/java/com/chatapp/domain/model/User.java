package com.chatapp.domain.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String externalId;

    private String displayName;
    private String avatarUrl;
    private String bio;
    private String fcmToken;

    @Builder.Default
    private boolean isOnline = false;

    private Instant lastSeen;

    @CreatedDate
    private Instant createdAt;
}
