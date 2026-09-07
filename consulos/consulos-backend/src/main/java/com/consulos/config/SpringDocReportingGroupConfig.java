package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocReportingGroupConfig {

    @Bean
    GroupedOpenApi mvpReportingApi() {
        return GroupedOpenApi.builder()
                .group("11-reporting")
                .pathsToMatch("/reporting/**")
                .build();
    }
}
