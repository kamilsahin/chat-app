package com.chatapp.service;

import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.RoomRepository;
import com.chatapp.domain.repository.UserRepository;
import com.chatapp.websocket.dto.PresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void onConnect(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            userRepository.save(user);
            broadcastPresence(user);
            log.debug("User connected: {}", userId);
        });
    }

    public void onDisconnect(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(false);
            user.setLastSeen(Instant.now());
            userRepository.save(user);
            broadcastPresence(user);
            log.debug("User disconnected: {}", userId);
        });
    }

    private void broadcastPresence(User user) {
        PresenceEvent event = new PresenceEvent(user.getId(), user.isOnline(), user.getLastSeen());

        List<String> roomIds = roomRepository.findAllByMemberUserId(user.getId())
                .stream().map(r -> r.getId()).toList();

        for (String roomId : roomIds) {
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".presence", event);
        }
    }
}
