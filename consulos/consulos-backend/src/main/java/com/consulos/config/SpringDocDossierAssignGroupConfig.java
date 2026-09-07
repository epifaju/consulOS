package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocDossierAssignGroupConfig {

    @Bean
    GroupedOpenApi mvpDossierAssignApi() {
        return GroupedOpenApi.builder()
                .group("03-dossiers-assign")
                .pathsToMatch("/dossier-assign/**", "/dossiers/*/assign-agent")
                .build();
    }
}
