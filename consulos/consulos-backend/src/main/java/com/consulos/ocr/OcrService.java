package com.consulos.ocr;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.ocr.dto.DocumentOcrResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class OcrService {

    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_FAILED = "FAILED";

    private final DocumentOcrResultRepository resultRepository;
    private final OcrDocumentLookupRepository documentLookupRepository;
    private final OcrMinioReader minioReader;
    private final OcrProvider ocrProvider;

    @Async
    @Transactional
    public void processAsyncByDocumentId(UUID documentId) {
        try {
            DocumentEntity document = documentLookupRepository.findByIdAndActifTrue(documentId).orElse(null);
            if (document == null) {
                return;
            }

            DocumentOcrResultEntity entity = resultRepository
                    .findByDocumentIdAndOrganisationId(documentId, document.getOrganisationId())
                    .orElseGet(() -> {
                        DocumentOcrResultEntity created = new DocumentOcrResultEntity();
                        created.setId(UUID.randomUUID());
                        created.setOrganisationId(document.getOrganisationId());
                        created.setDocumentId(documentId);
                        created.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        return created;
                    });

            byte[] content = minioReader.readObject(document.getCheminStockage());
            OcrExtractResult extract = ocrProvider.extract(
                    document.getNomOriginal(),
                    document.getMimeType(),
                    content
            );

            entity.setProvider(ocrProvider.providerName());
            entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            if (extract.success()) {
                entity.setStatus(STATUS_DONE);
                entity.setExtractedText(extract.text());
                entity.setErrorMessage(null);
            } else {
                entity.setStatus(STATUS_FAILED);
                entity.setExtractedText(null);
                entity.setErrorMessage(extract.errorMessage());
            }
            resultRepository.save(entity);
            log.info("OCR {} pour document {} ({})", entity.getStatus(), documentId, entity.getProvider());
        } catch (Exception ex) {
            log.warn("OCR async échoué pour document {}: {}", documentId, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public DocumentOcrResultDTO getResult(UUID documentId, UUID organisationId) {
        return resultRepository.findByDocumentIdAndOrganisationId(documentId, organisationId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(
                        "OCR_NOT_FOUND", "Aucun résultat OCR pour ce document", HttpStatus.NOT_FOUND));
    }

    private DocumentOcrResultDTO toDto(DocumentOcrResultEntity entity) {
        return new DocumentOcrResultDTO(
                entity.getDocumentId(),
                entity.getProvider(),
                entity.getStatus(),
                entity.getExtractedText(),
                entity.getErrorMessage(),
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }
}
