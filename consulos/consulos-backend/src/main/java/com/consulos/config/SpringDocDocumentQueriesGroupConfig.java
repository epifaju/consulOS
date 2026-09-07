package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocDocumentQueriesGroupConfig {

    @Bean
    GroupedOpenApi mvpDocumentQueriesApi() {
        return GroupedOpenApi.builder()
                .group("05-documents-queries")
                .pathsToMatch("/document-queries/**")
                .build();
    }
}
