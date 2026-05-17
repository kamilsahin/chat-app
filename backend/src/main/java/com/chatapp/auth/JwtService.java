package com.chatapp.auth;

import com.chatapp.config.ChatProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final ChatProperties properties;

    public Optional<Claims> parse(String token) {
        try {
            SecretKey key = buildKey(properties.getAuth().getJwtSecret());

            var parser = Jwts.parser().verifyWith(key);

            String issuer = properties.getAuth().getJwtIssuer();
            if (issuer != null && !issuer.isBlank()) {
                parser.requireIssuer(issuer);
            }

            Claims claims = parser.build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractExternalId(String token) {
        return parse(token).map(Claims::getSubject);
    }

    // ActiZone (JJWT 0.9.x) calls signWith(HS512, base64String) which base64-decodes
    // the secret before using it as the HMAC key. We replicate that here so the chat
    // service can verify tokens issued by ActiZone without changing ActiZone.
    private SecretKey buildKey(String secret) {
        int padding = (4 - secret.length() % 4) % 4;
        byte[] keyBytes = Base64.getDecoder().decode(secret + "=".repeat(padding));
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }
}
