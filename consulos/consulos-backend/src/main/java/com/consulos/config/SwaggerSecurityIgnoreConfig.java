package com.consulos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class SwaggerSecurityIgnoreConfig {

    @Bean
    public WebSecurityCustomizer swaggerWebSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs",
                        "/api-docs/**",
                        "/v3/api-docs/**"
                );
    }
}
