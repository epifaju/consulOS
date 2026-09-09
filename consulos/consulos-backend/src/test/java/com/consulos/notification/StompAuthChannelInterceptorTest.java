package com.consulos.notification;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.auth.JwtService;
import com.consulos.auth.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    private ConsulosUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new ConsulosUserDetails(
                UUID.randomUUID(),
                "agent@consulos.local",
                "hash",
                UUID.randomUUID(),
                "ROLE_AGENT",
                true
        );
    }

    @Test
    void connect_whenMissingToken_shouldThrow() {
        Message<byte[]> message = connectMessage(null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT");
        verifyNoInteractions(jwtService);
    }

    @Test
    void connect_whenInvalidToken_shouldThrow() {
        Message<byte[]> message = connectMessage("Bearer bad-token", null);
        when(jwtService.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT");
    }

    @Test
    void connect_whenValidBearerToken_shouldSetUser() {
        Message<byte[]> message = connectMessage("Bearer good-token", null);
        when(jwtService.validateToken("good-token")).thenReturn(true);
        when(jwtService.extractEmail("good-token")).thenReturn("agent@consulos.local");
        when(userDetailsService.loadUserByUsername("agent@consulos.local")).thenReturn(userDetails);

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("agent@consulos.local");
    }

    @Test
    void connect_whenAccessTokenNativeHeader_shouldAuthenticate() {
        Message<byte[]> message = connectMessage(null, "native-token");
        when(jwtService.validateToken("native-token")).thenReturn(true);
        when(jwtService.extractEmail("native-token")).thenReturn("agent@consulos.local");
        when(userDetailsService.loadUserByUsername("agent@consulos.local")).thenReturn(userDetails);

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void nonConnect_shouldPassThroughWithoutJwt() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(jwtService, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(userDetailsService);
    }

    private static Message<byte[]> connectMessage(String authorization, String accessToken) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (accessToken != null) {
            accessor.setNativeHeader("access_token", accessToken);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
