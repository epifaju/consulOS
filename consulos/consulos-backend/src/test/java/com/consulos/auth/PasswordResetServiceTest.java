package com.consulos.auth;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String RESET_SECRET = "unit-test-reset-secret";
    private static final String KEY_PREFIX = "pwd_reset:";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "resetSecret", RESET_SECRET);
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@consulos.local");
    }

    @Test
    void createResetToken_whenUserExists_shouldStoreSignedTokenInRedis() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        Optional<String> token = passwordResetService.createResetToken(user.getEmail());

        assertThat(token).isPresent();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofHours(1)));
        assertThat(keyCaptor.getValue()).isEqualTo(KEY_PREFIX + token.get());
        assertThat(valueCaptor.getValue()).isEqualTo(user.getId().toString());
        assertThat(isValidSignature(token.get())).isTrue();
    }

    @Test
    void createResetToken_whenUserUnknown_shouldReturnEmptyWithoutRedis() {
        when(userRepository.findByEmail("unknown@consulos.local")).thenReturn(Optional.empty());

        Optional<String> token = passwordResetService.createResetToken("unknown@consulos.local");

        assertThat(token).isEmpty();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void validateAndConsumeToken_whenValid_shouldReturnUserIdAndDeleteKey() {
        String token = createSignedToken(user.getId(), "random-part");
        when(valueOperations.get(KEY_PREFIX + token)).thenReturn(user.getId().toString());

        Optional<UUID> result = passwordResetService.validateAndConsumeToken(token);

        assertThat(result).contains(user.getId());
        verify(redisTemplate).delete(KEY_PREFIX + token);
    }

    @Test
    void validateAndConsumeToken_whenMissingInRedis_shouldReturnEmpty() {
        String token = createSignedToken(user.getId(), "random-part");
        when(valueOperations.get(KEY_PREFIX + token)).thenReturn(null);

        assertThat(passwordResetService.validateAndConsumeToken(token)).isEmpty();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validateAndConsumeToken_whenSignatureTampered_shouldReturnEmpty() {
        String validToken = createSignedToken(user.getId(), "random-part");
        String tampered = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("tampered:payload:badsig".getBytes(StandardCharsets.UTF_8));
        when(valueOperations.get(KEY_PREFIX + tampered)).thenReturn(user.getId().toString());

        assertThat(passwordResetService.validateAndConsumeToken(tampered)).isEmpty();
        verify(redisTemplate, never()).delete(anyString());
        assertThat(isValidSignature(validToken)).isTrue();
    }

    @Test
    void validateAndConsumeToken_whenAlreadyConsumed_shouldNotReuse() {
        String token = createSignedToken(user.getId(), "once");
        when(valueOperations.get(KEY_PREFIX + token)).thenReturn(user.getId().toString(), (String) null);

        assertThat(passwordResetService.validateAndConsumeToken(token)).contains(user.getId());
        assertThat(passwordResetService.validateAndConsumeToken(token)).isEmpty();
    }

    @Test
    void invalidateToken_shouldDeleteRedisKey() {
        passwordResetService.invalidateToken("any-token");

        verify(redisTemplate).delete(KEY_PREFIX + "any-token");
    }

    private boolean isValidSignature(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int lastColon = decoded.lastIndexOf(':');
            String payload = decoded.substring(0, lastColon);
            String signature = decoded.substring(lastColon + 1);
            return hmac(payload).equals(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    private String createSignedToken(UUID userId, String random) {
        String payload = userId + ":" + random;
        String signature = hmac(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    RESET_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
