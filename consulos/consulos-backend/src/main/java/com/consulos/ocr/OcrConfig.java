package com.consulos.ocr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OcrProperties.class)
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class OcrConfig {

    @Bean
    @ConditionalOnProperty(prefix = "consulos.ocr", name = "provider", havingValue = "log", matchIfMissing = true)
    OcrProvider loggingOcrProvider() {
        return new LoggingOcrProvider();
    }
}
