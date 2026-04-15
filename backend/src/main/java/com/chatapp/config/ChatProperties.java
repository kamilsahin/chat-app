package com.chatapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private Auth auth = new Auth();
    private Internal internal = new Internal();
    private Storage storage = new Storage();
    private Notifications notifications = new Notifications();
    private Features features = new Features();

    @Data
    public static class Auth {
        private String jwtSecret;
        private String jwtIssuer;
    }

    @Data
    public static class Internal {
        private String apiSecret;
    }

    @Data
    public static class Storage {
        private String provider = "LOCAL";
        private Local local = new Local();
        private S3 s3 = new S3();
        private R2 r2 = new R2();

        @Data
        public static class Local {
            private String uploadDir = "/uploads";
        }

        @Data
        public static class S3 {
            private String bucket;
            private String region;
            private String accessKey;
            private String secretKey;
        }

        @Data
        public static class R2 {
            private String bucket;
            private String accountId;
            private String accessKey;
            private String secretKey;
        }
    }

    @Data
    public static class Notifications {
        private boolean enabled = false;
        private Fcm fcm = new Fcm();

        @Data
        public static class Fcm {
            private String credentialsFile;
        }
    }

    @Data
    public static class Features {
        private boolean fileSharingEnabled = true;
        private boolean reactionsEnabled = true;
        private int maxMessageLength = 2000;
        private int maxGroupSize = 500;
        private int messageRetentionDays = 0;
        private String deletePermission = "OWN_ONLY";
        private List<String> keywordBlocklist = List.of();
    }
}
