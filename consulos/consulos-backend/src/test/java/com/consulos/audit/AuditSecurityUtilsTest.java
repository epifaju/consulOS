package com.consulos.audit;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditSecurityUtilsTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORG_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrganisationId_whenConsulosPrincipal_returnsOrgId() {
        setConsulosAuthentication();

        assertThat(AuditSecurityUtils.getOrganisationId()).isEqualTo(ORG_ID);
    }

    @Test
    void getOrganisationId_whenNoAuthentication_throwsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(AuditSecurityUtils::getOrganisationId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("UNAUTHORIZED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void getOrganisationId_whenNonConsulosPrincipal_throwsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null)
        );

        assertThatThrownBy(AuditSecurityUtils::getOrganisationId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("UNAUTHORIZED"));
    }

    private static void setConsulosAuthentication() {
        ConsulosUserDetails details = new ConsulosUserDetails(
                USER_ID,
                "audit@consulos.local",
                "n/a",
                ORG_ID,
                "ROLE_ADMIN",
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );
    }
}
