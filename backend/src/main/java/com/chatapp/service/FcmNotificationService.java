package com.chatapp.service;

import com.chatapp.config.ChatProperties;
import com.chatapp.domain.model.Message;
import com.chatapp.domain.model.Room;
import com.chatapp.domain.model.User;
import com.chatapp.domain.repository.RoomRepository;
import com.chatapp.domain.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final ChatProperties properties;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final Optional<FirebaseMessaging> firebaseMessaging;

    @Async
    public void notifyNewMessage(Message message, String senderDisplayName) {
        try {
            if (!properties.getNotifications().isEnabled()) return;
            if (firebaseMessaging.isEmpty()) return;

            Room room = roomRepository.findById(message.getRoomId()).orElse(null);
            if (room == null) return;

            String title = senderDisplayName != null && !senderDisplayName.isBlank()
                    ? senderDisplayName : "Yeni mesaj";
            String body = buildBody(message);

            // Sender'ın avatarını çek — bildirime eklenecek
            String senderAvatarUrl = userRepository.findByExternalId(message.getSenderId())
                    .map(User::getAvatarUrl)
                    .orElse(null);

            List<String> recipientIds = room.getMembers().stream()
                    .filter(m -> !m.getUserId().equals(message.getSenderId()))
                    .filter(m -> !isEffectivelyMuted(m))
                    .map(Room.Member::getUserId)
                    .toList();

            if (recipientIds.isEmpty()) return;

            userRepository.findAllByExternalIdIn(recipientIds).forEach(recipient -> {
                if (recipient.getFcmToken() != null && !recipient.getFcmToken().isBlank()) {
                    sendToToken(recipient, title, body, message.getRoomId(), senderAvatarUrl);
                }
            });
        } catch (Exception e) {
            log.error("[FCM] notifyNewMessage failed — messageId={}: {}", message.getId(), e.getMessage(), e);
        }
    }

    private void sendToToken(User user, String title, String body, String roomId, String senderAvatarUrl) {
        try {
            var builder = com.google.firebase.messaging.Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putData("roomId", roomId);

            if (senderAvatarUrl != null && !senderAvatarUrl.isBlank()) {
                builder.putData("avatarUrl", senderAvatarUrl);
            }

            com.google.firebase.messaging.Message fcmMessage = builder.build();

            firebaseMessaging.get().send(fcmMessage);
        } catch (FirebaseMessagingException e) {
            if (MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
                log.warn("FCM token geçersiz, temizleniyor: userId={}", user.getExternalId());
                user.setFcmToken(null);
                userRepository.save(user);
            } else {
                log.error("FCM send hatası: userId={} error={}", user.getExternalId(), e.getMessage());
            }
        }
    }

    private String buildBody(Message message) {
        if (message.getType() == Message.MessageType.IMAGE) return "📷 Fotoğraf";
        String content = message.getContent();
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "…" : content;
    }

    private boolean isEffectivelyMuted(Room.Member member) {
        if (!member.isMuted()) return false;
        if (member.getMutedUntil() == null) return true;
        return Instant.now().isBefore(member.getMutedUntil());
    }
}
