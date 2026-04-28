package com.chatapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "chat.notifications.enabled", havingValue = "true")
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(ChatProperties properties) throws IOException {
        String credentialsFile = properties.getNotifications().getFcm().getCredentialsFile();
        if (credentialsFile == null || credentialsFile.isBlank()) {
            throw new IllegalStateException(
                    "chat.notifications.fcm.credentials-file must be set when notifications are enabled");
        }

        InputStream credentialsStream = credentialsFile.startsWith("classpath:")
                ? new ClassPathResource(credentialsFile.substring("classpath:".length())).getInputStream()
                : new FileInputStream(credentialsFile);

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();

        FirebaseApp app;
        try {
            app = FirebaseApp.getInstance("chat-app");
        } catch (IllegalStateException e) {
            app = FirebaseApp.initializeApp(options, "chat-app");
        }

        log.info("Firebase initialized with credentials: {}", credentialsFile);
        return FirebaseMessaging.getInstance(app);
    }
}
