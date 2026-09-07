package com.consulos.portal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class PortalSecurityIgnoreConfig {

    /**
     * Endpoints publics / session portail hors SecurityFilterChain staff.
     * L'auth est assurée par {@link PortalAuthFilter}.
     */
    @Bean
    WebSecurityCustomizer portalWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/portal/**");
    }
}
