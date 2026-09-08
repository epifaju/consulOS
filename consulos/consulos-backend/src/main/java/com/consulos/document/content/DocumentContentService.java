package com.consulos.document.content;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.DocumentRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentContentService {

    /** Aligné sur MinioStorageService.BUCKET_NAME (consulos-docs). */
    private static final String BUCKET_NAME = "consulos-docs";

    private final DocumentRepository documentRepository;
    private final MinioClient minioClient;

    public DocumentContentPayload open(UUID documentId, UUID organisationId) {
        DocumentEntity document = documentRepository
                .findByIdAndOrganisationIdAndActifTrue(documentId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND",
                        "Document introuvable",
                        HttpStatus.NOT_FOUND));

        String objectKey = document.getCheminStockage();
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .build());

            // Lire en mémoire (plafond upload = 10 Mo) pour éviter les soucis
            // InputStreamResource / Content-Length avec le flux MinIO.
            byte[] bytes;
            try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .build())) {
                bytes = stream.readAllBytes();
            }

            if (bytes.length == 0) {
                throw new BusinessException(
                        "DOCUMENT_FILE_MISSING",
                        "Fichier vide ou absent du stockage.",
                        HttpStatus.NOT_FOUND);
            }

            return new DocumentContentPayload(
                    bytes,
                    document.getNomOriginal(),
                    document.getMimeType() != null ? document.getMimeType() : "application/octet-stream"
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (ErrorResponseException ex) {
            String code = ex.errorResponse() != null ? ex.errorResponse().code() : null;
            if ("NoSuchKey".equals(code)) {
                log.warn("Fichier MinIO absent pour document {} clé={}", documentId, objectKey);
                throw new BusinessException(
                        "DOCUMENT_FILE_MISSING",
                        "Fichier absent du stockage. Ré-uploadez le document.",
                        HttpStatus.NOT_FOUND);
            }
            log.error("Erreur MinIO document {}: {}", documentId, ex.getMessage());
            throw new BusinessException(
                    "DOCUMENT_STORAGE_ERROR",
                    "Erreur de stockage document",
                    HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Lecture document {} échouée: {}", documentId, ex.toString(), ex);
            throw new BusinessException(
                    "DOCUMENT_STORAGE_ERROR",
                    "Impossible de lire le fichier document",
                    HttpStatus.BAD_GATEWAY);
        }
    }
}
