package com.consulos.notification;

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

class NotificationSecurityUtilsTest {

    private static final UUID USER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ORG_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrganisationId_andGetUserId_whenConsulosPrincipal_returnIds() {
        setConsulosAuthentication();

        assertThat(NotificationSecurityUtils.getOrganisationId()).isEqualTo(ORG_ID);
        assertThat(NotificationSecurityUtils.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void getOrganisationId_whenNoAuthentication_throwsUnauthorized() {
        assertThatThrownBy(NotificationSecurityUtils::getOrganisationId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("UNAUTHORIZED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void getUserId_whenNonConsulosPrincipal_throwsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null)
        );

        assertThatThrownBy(NotificationSecurityUtils::getUserId)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("UNAUTHORIZED"));
    }

    private static void setConsulosAuthentication() {
        ConsulosUserDetails details = new ConsulosUserDetails(
                USER_ID,
                "notif@consulos.local",
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
