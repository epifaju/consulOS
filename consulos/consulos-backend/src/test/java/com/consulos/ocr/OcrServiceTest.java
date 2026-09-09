package com.consulos.ocr;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.ocr.dto.DocumentOcrResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock
    private DocumentOcrResultRepository resultRepository;
    @Mock
    private OcrDocumentLookupRepository documentLookupRepository;
    @Mock
    private OcrMinioReader minioReader;
    @Mock
    private OcrProvider ocrProvider;

    @InjectMocks
    private OcrService ocrService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID documentId;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        document = new DocumentEntity();
        document.setId(documentId);
        document.setOrganisationId(organisationId);
        document.setNomOriginal("scan.pdf");
        document.setMimeType("application/pdf");
        document.setCheminStockage("orgs/doc/scan.pdf");
        document.setActif(true);
    }

    @Test
    void processAsyncByDocumentId_whenSuccess_shouldPersistDoneResult() {
        when(documentLookupRepository.findByIdAndActifTrue(documentId)).thenReturn(Optional.of(document));
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.empty());
        when(minioReader.readObject("orgs/doc/scan.pdf")).thenReturn(new byte[]{1, 2, 3});
        when(ocrProvider.providerName()).thenReturn("tesseract");
        when(ocrProvider.extract("scan.pdf", "application/pdf", new byte[]{1, 2, 3}))
                .thenReturn(OcrExtractResult.ok("NOM: Camara\nPRENOM: Mamadou"));
        when(resultRepository.save(any(DocumentOcrResultEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ocrService.processAsyncByDocumentId(documentId);

        ArgumentCaptor<DocumentOcrResultEntity> captor = ArgumentCaptor.forClass(DocumentOcrResultEntity.class);
        verify(resultRepository).save(captor.capture());
        DocumentOcrResultEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OcrService.STATUS_DONE);
        assertThat(saved.getProvider()).isEqualTo("tesseract");
        assertThat(saved.getExtractedText()).contains("Camara");
        assertThat(saved.getErrorMessage()).isNull();
        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getDocumentId()).isEqualTo(documentId);
    }

    @Test
    void processAsyncByDocumentId_whenExtractFails_shouldPersistFailedStatus() {
        when(documentLookupRepository.findByIdAndActifTrue(documentId)).thenReturn(Optional.of(document));
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.empty());
        when(minioReader.readObject(any())).thenReturn(new byte[]{9});
        when(ocrProvider.providerName()).thenReturn("log");
        when(ocrProvider.extract(any(), any(), any())).thenReturn(OcrExtractResult.failed("unreadable"));
        when(resultRepository.save(any(DocumentOcrResultEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ocrService.processAsyncByDocumentId(documentId);

        ArgumentCaptor<DocumentOcrResultEntity> captor = ArgumentCaptor.forClass(DocumentOcrResultEntity.class);
        verify(resultRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OcrService.STATUS_FAILED);
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("unreadable");
        assertThat(captor.getValue().getExtractedText()).isNull();
    }

    @Test
    void processAsyncByDocumentId_whenDocumentMissing_shouldNoOp() {
        when(documentLookupRepository.findByIdAndActifTrue(documentId)).thenReturn(Optional.empty());

        ocrService.processAsyncByDocumentId(documentId);

        verify(resultRepository, never()).save(any());
        verify(minioReader, never()).readObject(any());
    }

    @Test
    void getResult_shouldReturnMappedDtoForOrganisation() {
        DocumentOcrResultEntity entity = new DocumentOcrResultEntity();
        entity.setDocumentId(documentId);
        entity.setOrganisationId(organisationId);
        entity.setProvider("tesseract");
        entity.setStatus(OcrService.STATUS_DONE);
        entity.setExtractedText("texte");
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId))
                .thenReturn(Optional.of(entity));

        DocumentOcrResultDTO dto = ocrService.getResult(documentId, organisationId);

        assertThat(dto.documentId()).isEqualTo(documentId);
        assertThat(dto.status()).isEqualTo(OcrService.STATUS_DONE);
        assertThat(dto.extractedText()).isEqualTo("texte");
    }

    @Test
    void getResult_whenOtherOrganisation_shouldThrowNotFound() {
        when(resultRepository.findByDocumentIdAndOrganisationId(documentId, otherOrganisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ocrService.getResult(documentId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("OCR_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
