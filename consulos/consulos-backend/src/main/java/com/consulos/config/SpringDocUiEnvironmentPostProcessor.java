package com.consulos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class SpringDocUiEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("springdoc.swagger-ui.doc-expansion", "list");
        properties.put("springdoc.swagger-ui.display-request-duration", "true");
        properties.put("springdoc.swagger-ui.filter", "true");
        properties.put("springdoc.swagger-ui.try-it-out-enabled", "true");
        properties.put("springdoc.swagger-ui.tags-sorter", "alpha");
        properties.put("springdoc.swagger-ui.groups-order", "ASC");
        properties.put("springdoc.writer-with-order-by-keys", "true");
        environment.getPropertySources().addLast(new MapPropertySource("consulosSpringdocUi", properties));
    }
}
