package com.consulos.reporting;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class ReportingSecurityUtils {

    private ReportingSecurityUtils() {
    }

    public static UUID getOrganisationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ConsulosUserDetails userDetails)) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return userDetails.getOrganisationId();
    }
}
