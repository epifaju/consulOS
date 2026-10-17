package com.consulos.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Scrape Prometheus sur le réseau Docker interne. Nginx refuse l'accès public
 * à {@code /api/v1/actuator/prometheus}.
 */
@Configuration
@ConditionalOnProperty(name = "consulos.observability.prometheus-anonymous", havingValue = "true")
public class ActuatorPrometheusAnonymousSecurityConfig {

    @Bean
    public WebSecurityCustomizer prometheusAnonymousWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/actuator/prometheus");
    }
}
