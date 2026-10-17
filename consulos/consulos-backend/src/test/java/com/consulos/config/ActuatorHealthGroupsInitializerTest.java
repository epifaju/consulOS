package com.consulos.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorHealthGroupsInitializerTest {

    @Test
    void initialize_shouldEnableProbesAndSplitLivenessFromRedis() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            new ActuatorHealthGroupsInitializer().initialize(context);
            ConfigurableEnvironment env = context.getEnvironment();

            assertThat(env.getProperty("management.endpoint.health.probes.enabled")).isEqualTo("true");
            assertThat(env.getProperty("management.endpoint.health.group.liveness.include"))
                    .isEqualTo("ping,livenessState")
                    .doesNotContain("redis");
            assertThat(env.getProperty("management.endpoint.health.group.readiness.include"))
                    .contains("redis")
                    .contains("db");
        }
    }
}
