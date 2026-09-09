package com.consulos.ocr;

import com.consulos.document.StatutControle;
import com.consulos.document.dto.DocumentDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DocumentOcrAspectTest {

    @Mock
    private OcrService ocrService;

    @InjectMocks
    private DocumentOcrAspect aspect;

    @Test
    void afterDocumentUpload_withDocumentId_shouldProcessAsync() {
        UUID documentId = UUID.randomUUID();
        DocumentDTO dto = documentDto(documentId);

        aspect.afterDocumentUpload(dto);

        verify(ocrService).processAsyncByDocumentId(documentId);
    }

    @Test
    void afterDocumentUpload_whenDtoNull_shouldNeverProcess() {
        aspect.afterDocumentUpload(null);

        verifyNoInteractions(ocrService);
    }

    @Test
    void afterDocumentUpload_whenOcrThrows_shouldSwallowException() {
        UUID documentId = UUID.randomUUID();
        DocumentDTO dto = documentDto(documentId);
        doThrow(new RuntimeException("OCR down")).when(ocrService).processAsyncByDocumentId(documentId);

        aspect.afterDocumentUpload(dto);

        verify(ocrService).processAsyncByDocumentId(documentId);
    }

    @Test
    void afterDocumentUpload_whenDtoIdNull_shouldNeverProcess() {
        DocumentDTO dto = documentDto(null);

        aspect.afterDocumentUpload(dto);

        verify(ocrService, never()).processAsyncByDocumentId(org.mockito.ArgumentMatchers.any());
    }

    private static DocumentDTO documentDto(UUID id) {
        return new DocumentDTO(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "file.pdf",
                "original.pdf",
                "PASSEPORT",
                "application/pdf",
                100L,
                StatutControle.RECU,
                1,
                null,
                OffsetDateTime.now()
        );
    }
}
