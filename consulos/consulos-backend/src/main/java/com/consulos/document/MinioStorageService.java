package com.consulos.document;

import com.consulos.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketVersioningArgs;
import io.minio.http.Method;
import io.minio.messages.VersioningConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    static final String BUCKET_NAME = "consulos-docs";

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    private final MinioClient minioClient;

    @PostConstruct
    void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
                log.info("Bucket MinIO créé: {}", BUCKET_NAME);
            }
            minioClient.setBucketVersioning(
                    SetBucketVersioningArgs.builder()
                            .bucket(BUCKET_NAME)
                            .config(new VersioningConfiguration(VersioningConfiguration.Status.ENABLED, null))
                            .build()
            );
        } catch (Exception ex) {
            log.warn("Initialisation MinIO échouée (le service démarrera sans stockage): {}", ex.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, UUID organisationId, UUID dossierId) {
        String mimeType = file.getContentType();
        if (!StringUtils.hasText(mimeType) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException("MIME_NON_AUTORISE", "Type de fichier non autorisé", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("FICHIER_TROP_GRAND", "Fichier trop volumineux (max 10 Mo)", HttpStatus.BAD_REQUEST);
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "document";
        String sanitized = sanitizeFilename(originalName);
        String dossierSegment = dossierId != null ? dossierId.toString() : "sans-dossier";
        String objectKey = "orgs/%s/dossiers/%s/%s-%s".formatted(
                organisationId, dossierSegment, UUID.randomUUID(), sanitized
        );

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(mimeType)
                            .build()
            );
            log.info("Fichier uploadé dans MinIO: {}", objectKey);
            return objectKey;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Échec upload MinIO", ex);
            throw new BusinessException("UPLOAD_ECHOUE", "Échec du stockage du fichier", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String generatePresignedDownloadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(BUCKET_NAME)
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception ex) {
            log.error("Échec génération URL pré-signée pour {}", objectKey, ex);
            throw new BusinessException(
                    "URL_GENERATION_ECHOUEE",
                    "Impossible de générer l'URL de téléchargement",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectKey)
                            .build()
            );
            log.info("Fichier supprimé de MinIO: {}", objectKey);
        } catch (Exception ex) {
            log.error("Échec suppression MinIO pour {}", objectKey, ex);
            throw new BusinessException(
                    "SUPPRESSION_ECHOUEE",
                    "Échec de la suppression du fichier",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
