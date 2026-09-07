package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "consulos.sms", name = "enabled", havingValue = "true")
public class SpringDocSmsGroupConfig {

    @Bean
    GroupedOpenApi mvpSmsApi() {
        return GroupedOpenApi.builder()
                .group("12-sms")
                .pathsToMatch("/sms/**")
                .build();
    }
}
