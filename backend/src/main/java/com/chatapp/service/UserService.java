package com.chatapp.service;

import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.UserRepository;
import com.chatapp.internal.dto.SyncUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User syncUser(String externalId, SyncUserRequest request) {
        User user = userRepository.findByExternalId(externalId)
                .orElseGet(() -> User.builder().externalId(externalId).build());

        if (request.displayName() != null) user.setDisplayName(request.displayName());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        if (request.bio() != null) user.setBio(request.bio());

        return userRepository.save(user);
    }

    public void deleteByExternalId(String externalId) {
        userRepository.findByExternalId(externalId)
                .ifPresent(userRepository::delete);
    }

    public User getByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + externalId));
    }

    public void updateFcmToken(String externalId, String fcmToken) {
        User user = userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + externalId));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }
}
