package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocMvpGroupsConfig {

    @Bean
    GroupedOpenApi authApi() {
        return GroupedOpenApi.builder().group("01-auth").pathsToMatch("/auth/**").build();
    }

    @Bean
    GroupedOpenApi mvpUsagersApi() {
        return GroupedOpenApi.builder().group("02-usagers").pathsToMatch("/usagers/**").build();
    }

    @Bean
    GroupedOpenApi mvpDossiersApi() {
        return GroupedOpenApi.builder().group("03-dossiers").pathsToMatch("/dossiers/**", "/types-service/**").build();
    }

    @Bean
    GroupedOpenApi rendezvousApi() {
        return GroupedOpenApi.builder().group("04-rendezvous").pathsToMatch("/rendezvous/**", "/parametres/**").build();
    }

    @Bean
    GroupedOpenApi documentsApi() {
        return GroupedOpenApi.builder().group("05-documents").pathsToMatch("/documents/**").build();
    }

    @Bean
    GroupedOpenApi encaissementsApi() {
        return GroupedOpenApi.builder().group("06-encaissements").pathsToMatch("/encaissements/**", "/tarifs/**").build();
    }

    @Bean
    GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder().group("07-dashboard").pathsToMatch("/dashboard/**").build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("08-administration").pathsToMatch("/admin/**", "/audit/**").build();
    }
}
