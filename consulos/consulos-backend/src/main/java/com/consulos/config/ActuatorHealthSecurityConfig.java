package com.consulos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Probes liveness/readiness accessibles sans JWT (chemins imbriqués non couverts
 * par le matcher exact {@code /actuator/health} du SecurityConfig existant).
 */
@Configuration
public class ActuatorHealthSecurityConfig {

    @Bean
    public WebSecurityCustomizer actuatorHealthWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info"
        );
    }
}
