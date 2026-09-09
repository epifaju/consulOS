package com.consulos.ocr.suggestion;

import com.consulos.common.BusinessException;
import com.consulos.ocr.DocumentOcrResultEntity;
import com.consulos.ocr.DocumentOcrResultRepository;
import com.consulos.ocr.OcrService;
import com.consulos.ocr.dto.OcrFieldSuggestionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrSuggestionServiceTest {

    @Mock
    private DocumentOcrResultRepository resultRepository;

    @InjectMocks
    private OcrSuggestionService ocrSuggestionService;

    private UUID organisationId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    void parse_shouldExtractStructuredFieldsAndNormalizeDate() {
        String text = """
                NOM: Camara
                PRENOM: Mamadou
                DATE_NAISSANCE: 12/05/1990
                LIEU_NAISSANCE: Bissau
                NATIONALITE: Guinée-Bissau
                NUMERO_DOCUMENT: P12345678
                TYPE_DOCUMENT: Passeport
                """;

        OcrSuggestionService.ParsedFields fields = OcrSuggestionService.parse(text);

        assertThat(fields.nom).isEqualTo("Camara");
        assertThat(fields.prenom).isEqualTo("Mamadou");
        assertThat(fields.dateNaissance).isEqualTo("1990-05-12");
        assertThat(fields.lieuNaissance).isEqualTo("Bissau");
        assertThat(fields.nationalite).isEqualTo("GUI");
        assertThat(fields.numeroDocument).isEqualTo("P12345678");
        assertThat(fields.typeDocument).isEqualTo("PASSEPORT");
        assertThat(fields.isEmpty()).isFalse();
    }

    @Test
    void suggest_shouldReturnHighConfidenceWhenDoneAndParsed() {
        DocumentOcrResultEntity entity = new DocumentOcrResultEntity();
        entity.setDocumentId(documentId);
        entity.setProvider("tesseract");
        entity.setStatus(OcrService.STATUS_DONE);
        entity.setExtractedText("NOM: Diallo\nPRENOM: Fatou\nNATIONALITE: GNB\nNUMERO_DOCUMENT: CNI1");
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.of(entity));

        OcrFieldSuggestionDTO dto = ocrSuggestionService.suggest(documentId, organisationId);

        assertThat(dto.nom()).isEqualTo("Diallo");
        assertThat(dto.prenom()).isEqualTo("Fatou");
        assertThat(dto.nationalite()).isEqualTo("GNB");
        assertThat(dto.confidence()).isEqualTo(0.85);
    }

    @Test
    void suggest_whenDemoFallbackText_shouldFillCandidateFields() {
        DocumentOcrResultEntity entity = new DocumentOcrResultEntity();
        entity.setDocumentId(documentId);
        entity.setProvider("log");
        entity.setStatus(OcrService.STATUS_DONE);
        entity.setExtractedText("OCR stub — champs candidats CNI");
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.of(entity));

        OcrFieldSuggestionDTO dto = ocrSuggestionService.suggest(documentId, organisationId);

        assertThat(dto.typeDocument()).isEqualTo("CNI");
        assertThat(dto.numeroDocument()).isEqualTo("CNI998877");
        assertThat(dto.nom()).isEqualTo("Camara");
        assertThat(dto.confidence()).isEqualTo(0.85);
    }

    @Test
    void suggest_whenMissingResult_shouldThrowNotFound() {
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ocrSuggestionService.suggest(documentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("OCR_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void suggest_whenEmptyText_shouldReturnZeroConfidence() {
        DocumentOcrResultEntity entity = new DocumentOcrResultEntity();
        entity.setDocumentId(documentId);
        entity.setProvider("log");
        entity.setStatus(OcrService.STATUS_FAILED);
        entity.setExtractedText("");
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.of(entity));

        OcrFieldSuggestionDTO dto = ocrSuggestionService.suggest(documentId, organisationId);

        assertThat(dto.confidence()).isZero();
        assertThat(dto.nom()).isNull();
    }
}
