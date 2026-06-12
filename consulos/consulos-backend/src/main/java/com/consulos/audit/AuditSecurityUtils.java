package com.consulos.audit;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuditSecurityUtils {

    private AuditSecurityUtils() {
    }

    public static UUID getOrganisationId() {
        return getUserDetails().getOrganisationId();
    }

    private static ConsulosUserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsulosUserDetails userDetails)) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return userDetails;
    }
}
