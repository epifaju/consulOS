package com.consulos.rendezvous;

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

class RdvSecurityUtilsTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrganisationId_whenConsulosPrincipal_returnsOrgId() {
        setConsulosAuthentication();

        assertThat(RdvSecurityUtils.getOrganisationId()).isEqualTo(ORG_ID);
    }

    @Test
    void getUserId_whenConsulosPrincipal_returnsUserId() {
        setConsulosAuthentication();

        assertThat(RdvSecurityUtils.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void getOrganisationId_whenNoAuthentication_throwsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(RdvSecurityUtils::getOrganisationId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("UNAUTHORIZED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void getUserId_whenNoAuthentication_throwsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(RdvSecurityUtils::getUserId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("UNAUTHORIZED"));
    }

    @Test
    void getOrganisationId_whenNonConsulosPrincipal_throwsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null)
        );

        assertThatThrownBy(RdvSecurityUtils::getOrganisationId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("UNAUTHORIZED"));
    }

    @Test
    void getUserId_whenNonConsulosPrincipal_throwsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null)
        );

        assertThatThrownBy(RdvSecurityUtils::getUserId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("UNAUTHORIZED"));
    }

    private static void setConsulosAuthentication() {
        ConsulosUserDetails details = new ConsulosUserDetails(
                USER_ID,
                "agent@consulos.local",
                "n/a",
                ORG_ID,
                "ROLE_AGENT",
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );
    }
}
