package com.consulos.auth;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX = "pwd_reset:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @Value("${consulos.auth.password-reset-secret:consulos-reset-secret-change-me}")
    private String resetSecret;

    public Optional<String> createResetToken(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    String token = generateSignedToken(user);
                    redisTemplate.opsForValue().set(KEY_PREFIX + token, user.getId().toString(), RESET_TOKEN_TTL);
                    return token;
                });
    }

    public Optional<UUID> validateAndConsumeToken(String token) {
        String userId = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (userId == null || !isTokenSignatureValid(token)) {
            return Optional.empty();
        }
        redisTemplate.delete(KEY_PREFIX + token);
        return Optional.of(UUID.fromString(userId));
    }

    public void invalidateToken(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }

    private String generateSignedToken(UserEntity user) {
        byte[] randomBytes = new byte[16];
        try {
            SecureRandom.getInstanceStrong().nextBytes(randomBytes);
        } catch (Exception ex) {
            new SecureRandom().nextBytes(randomBytes);
        }
        String random = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String payload = user.getId() + ":" + random;
        String signature = hmacSha256(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    private boolean isTokenSignatureValid(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int lastColon = decoded.lastIndexOf(':');
            if (lastColon <= 0) {
                return false;
            }
            String payload = decoded.substring(0, lastColon);
            String signature = decoded.substring(lastColon + 1);
            return hmacSha256(payload).equals(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(resetSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Erreur HMAC", ex);
        }
    }
}
