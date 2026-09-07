package com.consulos.notification;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.auth.JwtService;
import com.consulos.auth.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (!StringUtils.hasText(token) || !jwtService.validateToken(token)) {
                throw new IllegalArgumentException("JWT WebSocket invalide ou manquant");
            }
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!(userDetails instanceof ConsulosUserDetails consulosUser) || !consulosUser.isEnabled()) {
                throw new IllegalArgumentException("Utilisateur WebSocket inactif");
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(consulosUser, null, consulosUser.getAuthorities());
            accessor.setUser(authentication);
            log.debug("STOMP CONNECT authentifié pour {}", email);
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (StringUtils.hasText(accessToken)) {
            return accessToken.trim();
        }
        return null;
    }
}
