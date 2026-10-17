package com.consulos.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActuatorPrometheusAnonymousSecurityConfigTest {

    @Test
    void customizerBean_presentWhenPropertyTrue() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of("consulos.observability.prometheus-anonymous=true").applyTo(context);
            context.register(ActuatorPrometheusAnonymousSecurityConfig.class);
            context.refresh();

            assertThat(context.getBean(WebSecurityCustomizer.class)).isNotNull();
        }
    }

    @Test
    void customizerBean_absentWhenPropertyFalse() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of("consulos.observability.prometheus-anonymous=false").applyTo(context);
            context.register(ActuatorPrometheusAnonymousSecurityConfig.class);
            context.refresh();

            assertThatThrownBy(() -> context.getBean(WebSecurityCustomizer.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }
}
