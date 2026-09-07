package com.consulos.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
public class SpringDocPaymentGroupConfig {

    @Bean
    GroupedOpenApi mvpPaymentApi() {
        return GroupedOpenApi.builder()
                .group("13-payments")
                .pathsToMatch("/payments/**")
                .build();
    }
}
