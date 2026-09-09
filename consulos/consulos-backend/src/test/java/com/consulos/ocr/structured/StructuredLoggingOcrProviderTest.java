package com.consulos.ocr.structured;

import com.consulos.ocr.OcrExtractResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredLoggingOcrProviderTest {

    private final StructuredLoggingOcrProvider provider = new StructuredLoggingOcrProvider();

    @Test
    void providerName_shouldBeStructuredLog() {
        assertThat(provider.providerName()).isEqualTo("structured-log");
    }

    @Test
    void extract_whenFileContainsCamaraCniMamadou_shouldFillStructuredFields() {
        String fileName = "cni-camara-mamadou.jpg";

        OcrExtractResult result = provider.extract(fileName, "image/jpeg", new byte[]{10, 20});

        assertThat(result.success()).isTrue();
        assertThat(result.text()).contains("NOM: Camara");
        assertThat(result.text()).contains("TYPE_DOCUMENT: CNI");
        assertThat(result.text()).contains("PRENOM: Mamadou");
    }
}
