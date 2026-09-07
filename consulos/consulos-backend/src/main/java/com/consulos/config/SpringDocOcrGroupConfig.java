package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class SpringDocOcrGroupConfig {

    @Bean
    GroupedOpenApi mvpOcrApi() {
        return GroupedOpenApi.builder()
                .group("14-ocr")
                .pathsToMatch("/ocr/**")
                .build();
    }
}
