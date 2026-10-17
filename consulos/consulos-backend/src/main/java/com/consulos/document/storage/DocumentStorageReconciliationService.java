package com.consulos.document.storage;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.document.storage.dto.DocumentStorageMissingItemDTO;
import com.consulos.document.storage.dto.DocumentStorageReconciliationDTO;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageReconciliationService {

    /** Aligné sur MinioStorageService.BUCKET_NAME. */
    static final String BUCKET_NAME = "consulos-docs";
    private static final int SAMPLE_LIMIT = 100;

    private final DocumentStorageReconciliationRepository repository;
    private final MinioClient minioClient;

    @Transactional(readOnly = true)
    public DocumentStorageReconciliationDTO reconcile(UUID organisationId) {
        List<DocumentEntity> documents = repository.findByOrganisationIdAndActifTrue(organisationId);
        Set<String> minioKeys = listOrganisationObjectKeys(organisationId);

        List<DocumentStorageMissingItemDTO> missing = new ArrayList<>();
        int present = 0;
        for (DocumentEntity document : documents) {
            String key = document.getCheminStockage();
            if (!StringUtils.hasText(key)) {
                missing.add(new DocumentStorageMissingItemDTO(
                        document.getId(), document.getNomOriginal(), key));
                continue;
            }
            if (minioKeys.contains(key)) {
                present++;
            } else {
                missing.add(new DocumentStorageMissingItemDTO(
                        document.getId(), document.getNomOriginal(), key));
            }
        }

        Set<String> dbKeys = new HashSet<>();
        for (DocumentEntity document : documents) {
            if (StringUtils.hasText(document.getCheminStockage())) {
                dbKeys.add(document.getCheminStockage());
            }
        }
        List<String> orphans = minioKeys.stream()
                .filter(key -> !dbKeys.contains(key))
                .sorted()
                .toList();

        log.info(
                "Réconciliation stockage org={} db={} minio={} manquants={} orphelins={}",
                organisationId, documents.size(), minioKeys.size(), missing.size(), orphans.size());

        return new DocumentStorageReconciliationDTO(
                documents.size(),
                present,
                missing.size(),
                orphans.size(),
                missing.stream().limit(SAMPLE_LIMIT).toList(),
                orphans.stream().limit(SAMPLE_LIMIT).toList()
        );
    }

    private Set<String> listOrganisationObjectKeys(UUID organisationId) {
        String prefix = "orgs/" + organisationId + "/";
        Set<String> keys = new HashSet<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(BUCKET_NAME)
                            .prefix(prefix)
                            .recursive(true)
                            .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item != null && StringUtils.hasText(item.objectName()) && !item.isDir()) {
                    keys.add(item.objectName());
                }
            }
            return keys;
        } catch (Exception ex) {
            log.error("Listage MinIO échoué bucket={} prefix={}", BUCKET_NAME, prefix, ex);
            throw new BusinessException(
                    "DOCUMENT_STORAGE_ERROR",
                    "Impossible de lister les fichiers MinIO",
                    HttpStatus.BAD_GATEWAY);
        }
    }
}
