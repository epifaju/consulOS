package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupedConfig {

    @Bean
    public GroupedOpenApi usagersApi() {
        return GroupedOpenApi.builder()
                .group("usagers")
                .displayName("Usagers")
                .packagesToScan("com.consulos.usager")
                .build();
    }

    @Bean
    public GroupedOpenApi dossiersApi() {
        return GroupedOpenApi.builder()
                .group("dossiers")
                .displayName("Dossiers")
                .packagesToScan("com.consulos.dossier")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("Toute l'API")
                .packagesToScan("com.consulos")
                .build();
    }
}
