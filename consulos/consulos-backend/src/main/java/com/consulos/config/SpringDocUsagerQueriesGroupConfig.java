package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocUsagerQueriesGroupConfig {

    @Bean
    GroupedOpenApi mvpUsagerQueriesApi() {
        return GroupedOpenApi.builder().group("02-usagers-queries").pathsToMatch("/usager-queries/**").build();
    }
}
