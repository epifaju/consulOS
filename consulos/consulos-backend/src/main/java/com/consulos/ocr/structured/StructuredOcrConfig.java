package com.consulos.ocr.structured;

import com.consulos.ocr.OcrProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class StructuredOcrConfig {

    @Bean
    @ConditionalOnProperty(prefix = "consulos.ocr", name = "provider", havingValue = "structured-log")
    OcrProvider structuredLoggingOcrProvider() {
        return new StructuredLoggingOcrProvider();
    }
}
