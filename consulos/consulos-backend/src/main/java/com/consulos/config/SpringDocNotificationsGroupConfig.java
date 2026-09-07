package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocNotificationsGroupConfig {

    @Bean
    GroupedOpenApi mvpNotificationsApi() {
        return GroupedOpenApi.builder()
                .group("09-notifications")
                .pathsToMatch("/notifications/**")
                .build();
    }
}
