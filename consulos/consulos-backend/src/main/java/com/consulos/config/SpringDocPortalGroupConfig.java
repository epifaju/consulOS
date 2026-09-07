package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocPortalGroupConfig {

    @Bean
    GroupedOpenApi mvpPortalApi() {
        return GroupedOpenApi.builder()
                .group("10-portal")
                .pathsToMatch("/portal/**")
                .build();
    }
}
