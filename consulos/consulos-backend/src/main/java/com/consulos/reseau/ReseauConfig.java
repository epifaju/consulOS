package com.consulos.reseau;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReseauProperties.class)
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class ReseauConfig {
}
