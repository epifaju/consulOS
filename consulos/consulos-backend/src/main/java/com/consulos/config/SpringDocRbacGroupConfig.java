package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocRbacGroupConfig {

    @Bean
    GroupedOpenApi rbacApi() {
        return GroupedOpenApi.builder()
                .group("12-rbac")
                .pathsToMatch("/rbac/**")
                .build();
    }
}
