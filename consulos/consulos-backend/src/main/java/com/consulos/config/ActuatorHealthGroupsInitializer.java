package com.consulos.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Liveness sans Redis/DB : un Redis down ne tue pas le processus pour Kubernetes.
 * Readiness inclut db + redis.
 */
public class ActuatorHealthGroupsInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String PROPERTY_SOURCE = "consulosActuatorHealth";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("management.endpoint.health.probes.enabled", true);
        properties.put("management.endpoint.health.group.liveness.include", "ping,livenessState");
        properties.put("management.endpoint.health.group.readiness.include", "ping,readinessState,db,redis");
        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE, properties));
    }
}
