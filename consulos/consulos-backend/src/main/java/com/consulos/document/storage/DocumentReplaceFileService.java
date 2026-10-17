package com.consulos.document.storage;

import com.consulos.common.BusinessException;
import com.consulos.document.Auditable;
import com.consulos.document.DocumentEntity;
import com.consulos.document.DocumentMapper;
import com.consulos.document.DocumentRepository;
import com.consulos.document.MinioStorageService;
import com.consulos.document.dto.DocumentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentReplaceFileService {

    private final DocumentRepository documentRepository;
    private final MinioStorageService minioStorageService;
    private final DocumentMapper documentMapper;

    @Transactional
    @Auditable(action = "REPLACE_DOCUMENT_FILE")
    public DocumentDTO replaceFile(UUID documentId, MultipartFile file, UUID organisationId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "DOCUMENT_FILE_EMPTY",
                    "Fichier vide ou manquant",
                    HttpStatus.BAD_REQUEST);
        }
        DocumentEntity document = documentRepository
                .findByIdAndOrganisationIdAndActifTrue(documentId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND",
                        "Document introuvable",
                        HttpStatus.NOT_FOUND));

        MultipartFile typedFile = InferredContentTypeMultipartFile.withInferredType(file);
        String previousKey = document.getCheminStockage();
        String newKey = minioStorageService.uploadFile(typedFile, organisationId, document.getDossierId());

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : document.getNomOriginal();
        document.setCheminStockage(newKey);
        document.setNomFichier(originalName);
        document.setNomOriginal(originalName);
        document.setMimeType(typedFile.getContentType());
        document.setTailleOctets(typedFile.getSize());
        int nextVersion = document.getVersion() == null ? 1 : document.getVersion() + 1;
        document.setVersion(nextVersion);

        DocumentEntity saved = documentRepository.save(document);
        if (StringUtils.hasText(previousKey) && !previousKey.equals(newKey)) {
            try {
                minioStorageService.deleteFile(previousKey);
            } catch (RuntimeException ex) {
                log.warn("Ancien objet MinIO non supprimé document={} clé={}: {}",
                        documentId, previousKey, ex.getMessage());
            }
        }
        log.info("Fichier document remplacé: {} → {}", documentId, newKey);
        return documentMapper.toDto(saved);
    }
}
