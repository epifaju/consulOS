package com.consulos.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private JwtService jwtService;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void saveRefreshToken_shouldStoreWithSevenDayTtl() {
        refreshTokenService.saveRefreshToken(userId, "refresh-token");

        verify(valueOperations).set("rt:" + userId, "refresh-token", Duration.ofDays(7));
    }

    @Test
    void validateRefreshToken_whenMatches_shouldReturnTrue() {
        when(valueOperations.get("rt:" + userId)).thenReturn("refresh-token");

        assertThat(refreshTokenService.validateRefreshToken(userId, "refresh-token")).isTrue();
    }

    @Test
    void validateRefreshToken_whenDifferent_shouldReturnFalse() {
        when(valueOperations.get("rt:" + userId)).thenReturn("other-token");

        assertThat(refreshTokenService.validateRefreshToken(userId, "refresh-token")).isFalse();
    }

    @Test
    void validateRefreshToken_whenMissing_shouldReturnFalse() {
        when(valueOperations.get("rt:" + userId)).thenReturn(null);

        assertThat(refreshTokenService.validateRefreshToken(userId, "refresh-token")).isFalse();
    }

    @Test
    void rotateRefreshToken_shouldDeleteOldAndStoreNew() {
        when(jwtService.generateRefreshToken()).thenReturn("new-refresh");

        String rotated = refreshTokenService.rotateRefreshToken(userId);

        assertThat(rotated).isEqualTo("new-refresh");
        verify(redisTemplate).delete("rt:" + userId);
        verify(valueOperations).set(eq("rt:" + userId), eq("new-refresh"), eq(Duration.ofDays(7)));
    }

    @Test
    void revokeRefreshToken_shouldDeleteKey() {
        refreshTokenService.revokeRefreshToken(userId);

        verify(redisTemplate).delete("rt:" + userId);
    }
}
