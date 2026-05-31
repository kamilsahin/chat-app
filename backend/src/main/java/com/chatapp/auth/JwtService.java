package com.chatapp.auth;

import com.chatapp.config.ChatProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final ChatProperties properties;
    private final ObjectMapper objectMapper;

    public Optional<Map<String, Object>> parse(String token) {
        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length != 3) return Optional.empty();

            // JJWT 0.9.x (used by Actizone) base64-decodes the secret string
            // before signing. We must do the same to verify their tokens.
            byte[] keyBytes = Base64.getDecoder().decode(
                    properties.getAuth().getJwtSecret().getBytes(StandardCharsets.UTF_8));


            Map<String, Object> header = decodeJson(parts[0]);
            String jcaName = jcaName((String) header.getOrDefault("alg", "HS256"));

            Mac mac = Mac.getInstance(jcaName);
            mac.init(new SecretKeySpec(keyBytes, jcaName));
            byte[] expected = mac.doFinal(
                    (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            byte[] actual = base64UrlDecode(parts[2]);

            if (!MessageDigest.isEqual(expected, actual)) {
                log.warn("JWT signature mismatch");
                return Optional.empty();
            }

            Map<String, Object> payload = decodeJson(parts[1]);

            Object expObj = payload.get("exp");
            if (expObj instanceof Number exp && new Date(exp.longValue() * 1000L).before(new Date())) {
                log.warn("JWT expired");
                return Optional.empty();
            }

            String issuer = properties.getAuth().getJwtIssuer();
            if (issuer != null && !issuer.isBlank() && !issuer.equals(payload.get("iss"))) {
                log.warn("JWT issuer mismatch");
                return Optional.empty();
            }

            return Optional.of(payload);
        } catch (Exception e) {
            log.warn("JWT parse failed ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractExternalId(String token) {
        return parse(token).map(payload -> {
            Object sub = payload.get("sub");
            if (sub != null) return String.valueOf(sub);
            Object id = payload.get("id");
            return id != null ? String.valueOf(id) : null;
        }).filter(id -> id != null && !id.isBlank());
    }

    private byte[] base64UrlDecode(String s) {
        int pad = (4 - s.length() % 4) % 4;
        return Base64.getUrlDecoder().decode(s + "=".repeat(pad));
    }

    private Map<String, Object> decodeJson(String base64url) throws Exception {
        return objectMapper.readValue(base64UrlDecode(base64url), new TypeReference<>() {});
    }

    private String jcaName(String alg) {
        return switch (alg) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            default      -> "HmacSHA512";
        };
    }
}
