package com.consulos.testsupport;

import com.consulos.auth.ConsulosUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.UUID;

public class WithConsulosUserSecurityContextFactory implements WithSecurityContextFactory<WithConsulosUser> {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Override
    public SecurityContext createSecurityContext(WithConsulosUser annotation) {
        ConsulosUserDetails details = new ConsulosUserDetails(
                USER_ID,
                annotation.email(),
                "n/a",
                ORG_ID,
                annotation.role(),
                true
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details,
                null,
                details.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
