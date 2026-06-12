package com.consulos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Remplace l'URL PostgreSQL par défaut (localhost:5432) par le port Docker 5433
 * pour éviter le conflit avec une instance PostgreSQL Windows locale.
 */
public class DatasourceDevEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DEFAULT_DEV_URL = "jdbc:postgresql://127.0.0.1:5433/consulos_dev";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isBlank(environment.getProperty("POSTGRES_URL"))) {
            return;
        }

        String currentUrl = environment.getProperty("spring.datasource.url");
        if (!shouldOverrideDevUrl(currentUrl)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", DEFAULT_DEV_URL);
        environment.getPropertySources().addFirst(new MapPropertySource("consulosDevDatasource", properties));
    }

    private static boolean shouldOverrideDevUrl(String url) {
        if (isBlank(url)) {
            return true;
        }
        return url.contains("localhost:5432") || url.contains("127.0.0.1:5432");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
