package com.consulos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class SpringDocEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("springdoc.api-docs.path", "/api-docs");
        properties.put("springdoc.swagger-ui.path", "/swagger-ui");
        properties.put("springdoc.swagger-ui.operations-sorter", "alpha");
        properties.put("springdoc.swagger-ui.tags-sorter", "alpha");
        properties.put("springdoc.show-actuator", "false");
        environment.getPropertySources().addLast(new MapPropertySource("consulosSpringdoc", properties));
    }
}
