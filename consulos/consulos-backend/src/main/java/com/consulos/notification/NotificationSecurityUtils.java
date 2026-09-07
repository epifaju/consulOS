package com.consulos.notification;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class NotificationSecurityUtils {

    private NotificationSecurityUtils() {
    }

    public static UUID getOrganisationId() {
        return getUserDetails().getOrganisationId();
    }

    public static UUID getUserId() {
        return getUserDetails().getUserId();
    }

    private static ConsulosUserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsulosUserDetails userDetails)) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return userDetails;
    }
}
