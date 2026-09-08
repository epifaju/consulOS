package com.consulos.ocr.tesseract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcrTextStructurerTest {

    @Test
    void enrich_shouldAppendStructuredFieldsWhenLabelsPresent() {
        String raw = """
                REPUBLIQUE
                NOM: CAMARA
                PRENOM: MAMADOU
                Date de naissance 12/05/1990
                Nationalité GNB
                Passeport PAB123456
                """;

        String enriched = OcrTextStructurer.enrich(raw, "passeport_camara.pdf");

        assertThat(enriched).contains("NOM: Camara");
        assertThat(enriched).contains("PRENOM: Mamadou");
        assertThat(enriched).contains("DATE_NAISSANCE: 1990-05-12");
        assertThat(enriched).contains("NATIONALITE: GNB");
        assertThat(enriched).contains("TYPE_DOCUMENT: PASSEPORT");
    }
}
