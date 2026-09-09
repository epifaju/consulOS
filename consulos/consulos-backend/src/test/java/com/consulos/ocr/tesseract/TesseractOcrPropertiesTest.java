package com.consulos.ocr.tesseract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TesseractOcrPropertiesTest {

    @Test
    void defaults_shouldMatchExpectedValues() {
        TesseractOcrProperties properties = new TesseractOcrProperties();

        assertThat(properties.getDatapath()).isEqualTo("tessdata");
        assertThat(properties.getLanguage()).isEqualTo("fra+eng");
        assertThat(properties.getDpi()).isEqualTo(300);
    }

    @Test
    void setters_shouldUpdateValues() {
        TesseractOcrProperties properties = new TesseractOcrProperties();

        properties.setDatapath("/opt/tessdata");
        properties.setLanguage("eng");
        properties.setDpi(150);

        assertThat(properties.getDatapath()).isEqualTo("/opt/tessdata");
        assertThat(properties.getLanguage()).isEqualTo("eng");
        assertThat(properties.getDpi()).isEqualTo(150);
    }
}
