package com.consulos.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String KEY_PREFIX = "rt:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void saveRefreshToken(UUID userId, String token) {
        redisTemplate.opsForValue().set(key(userId), token, REFRESH_TOKEN_TTL);
    }

    public boolean validateRefreshToken(UUID userId, String token) {
        String stored = redisTemplate.opsForValue().get(key(userId));
        return stored != null && stored.equals(token);
    }

    public String rotateRefreshToken(UUID userId) {
        redisTemplate.delete(key(userId));
        String newToken = jwtService.generateRefreshToken();
        saveRefreshToken(userId, newToken);
        return newToken;
    }

    public void revokeRefreshToken(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
