package com.consulos.reseau;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class ReseauSecurityUtils {

    private ReseauSecurityUtils() {
    }

    public static ConsulosUserDetails requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsulosUserDetails userDetails)) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return userDetails;
    }

    public static UUID getUserId() {
        return requireUser().getUserId();
    }

    public static UUID getOrganisationId() {
        return requireUser().getOrganisationId();
    }
}
