package com.consulos.ocr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingOcrProviderTest {

    private final LoggingOcrProvider provider = new LoggingOcrProvider();

    @Test
    void extract_shouldReturnOkTextContainingFileNameAndSize() {
        byte[] content = new byte[]{1, 2, 3, 4, 5};
        String fileName = "passeport-scan.pdf";

        OcrExtractResult result = provider.extract(fileName, "application/pdf", content);

        assertThat(result.success()).isTrue();
        assertThat(result.text()).contains(fileName);
        assertThat(result.text()).contains(String.valueOf(content.length));
        assertThat(provider.providerName()).isEqualTo("log");
    }
}
