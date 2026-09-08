package com.consulos.ocr.tesseract;

import com.consulos.ocr.OcrProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TesseractOcrProperties.class)
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class TesseractOcrConfig {

    @Bean
    @ConditionalOnProperty(prefix = "consulos.ocr", name = "provider", havingValue = "tesseract")
    OcrProvider tesseractOcrProvider(TesseractOcrProperties properties) {
        return new TesseractOcrProvider(properties);
    }
}
