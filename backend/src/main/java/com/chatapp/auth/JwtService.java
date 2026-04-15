package com.chatapp.auth;

import com.chatapp.config.ChatProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final ChatProperties properties;

    public Optional<Claims> parse(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    properties.getAuth().getJwtSecret().getBytes(StandardCharsets.UTF_8));

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
}
