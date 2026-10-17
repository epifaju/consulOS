package com.consulos.document.storage;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.DocumentRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Empêche la génération d'une URL MinIO pré-signée si l'objet n'existe plus
 * (évite d'afficher du XML NoSuchKey dans le navigateur).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DocumentPresignedUrlGuardAspect {

    static final String BUCKET_NAME = DocumentStorageReconciliationService.BUCKET_NAME;

    private final DocumentRepository documentRepository;
    private final MinioClient minioClient;

    @Around("execution(* com.consulos.document.DocumentService.getDownloadUrl(..))")
    public Object ensureObjectExists(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID documentId = (UUID) joinPoint.getArgs()[0];
        UUID organisationId = (UUID) joinPoint.getArgs()[1];
        DocumentEntity document = documentRepository
                .findByIdAndOrganisationIdAndActifTrue(documentId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND",
                        "Document introuvable",
                        HttpStatus.NOT_FOUND));
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(document.getCheminStockage())
                    .build());
        } catch (ErrorResponseException ex) {
            String code = ex.errorResponse() != null ? ex.errorResponse().code() : null;
            if ("NoSuchKey".equals(code)) {
                log.warn("URL pré-signée refusée, objet MinIO absent document={} clé={}",
                        documentId, document.getCheminStockage());
                throw new BusinessException(
                        "DOCUMENT_FILE_MISSING",
                        "Fichier absent du stockage. Ré-uploadez le document.",
                        HttpStatus.NOT_FOUND);
            }
            throw new BusinessException(
                    "DOCUMENT_STORAGE_ERROR",
                    "Erreur de stockage document",
                    HttpStatus.BAD_GATEWAY);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Stat MinIO échoué pour document {}", documentId, ex);
            throw new BusinessException(
                    "DOCUMENT_STORAGE_ERROR",
                    "Impossible de vérifier le fichier document",
                    HttpStatus.BAD_GATEWAY);
        }
        return joinPoint.proceed();
    }
}
