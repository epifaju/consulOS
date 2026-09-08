package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class SpringDocRegionalGroupConfig {

    @Bean
    GroupedOpenApi mvpRegionalApi() {
        return GroupedOpenApi.builder()
                .group("15-regional")
                .pathsToMatch("/regional/**")
                .build();
    }
}
