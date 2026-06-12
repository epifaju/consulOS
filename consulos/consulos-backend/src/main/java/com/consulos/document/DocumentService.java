package com.consulos.document;

import com.consulos.common.BusinessException;
import com.consulos.document.dto.DocumentDTO;
import com.consulos.document.dto.DownloadUrlDTO;
import com.consulos.document.dto.StatutControleDTO;
import com.consulos.dossier.DossierRepository;
import com.consulos.usager.UsagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final int PRESIGNED_EXPIRY_MINUTES = 15;

    private final DocumentRepository documentRepository;
    private final DossierRepository dossierRepository;
    private final UsagerRepository usagerRepository;
    private final MinioStorageService minioStorageService;
    private final DocumentMapper documentMapper;

    @Transactional
    @Auditable(action = "UPLOAD_DOCUMENT")
    public DocumentDTO upload(
            MultipartFile file,
            UUID dossierId,
            UUID usagerId,
            String typeDocument,
            String notes,
            UUID uploaderUserId,
            UUID organisationId
    ) {
        if (dossierId != null) {
            dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId)
                    .orElseThrow(() -> new BusinessException(
                            "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
        }
        if (usagerId != null) {
            usagerRepository.findById(usagerId)
                    .filter(u -> organisationId.equals(u.getOrganisationId()) && Boolean.TRUE.equals(u.getActif()))
                    .orElseThrow(() -> new BusinessException(
                            "USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        }

        String objectKey = minioStorageService.uploadFile(file, organisationId, dossierId);
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "document";

        DocumentEntity entity = new DocumentEntity();
        entity.setOrganisationId(organisationId);
        entity.setDossierId(dossierId);
        entity.setUsagerId(usagerId);
        entity.setNomFichier(originalName);
        entity.setNomOriginal(originalName);
        entity.setTypeDocument(typeDocument);
        entity.setMimeType(file.getContentType());
        entity.setTailleOctets(file.getSize());
        entity.setCheminStockage(objectKey);
        entity.setStatutControle(StatutControle.RECU);
        entity.setNotes(notes);
        entity.setCreatedBy(uploaderUserId);
        entity.setActif(true);

        DocumentEntity saved = documentRepository.save(entity);
        log.info("Document enregistré: {} ({})", saved.getId(), saved.getNomOriginal());
        return documentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public DocumentDTO findById(UUID documentId, UUID organisationId) {
        return documentMapper.toDto(getDocumentForOrganisation(documentId, organisationId));
    }

    @Transactional(readOnly = true)
    public DownloadUrlDTO getDownloadUrl(UUID documentId, UUID organisationId) {
        DocumentEntity document = getDocumentForOrganisation(documentId, organisationId);
        String url = minioStorageService.generatePresignedDownloadUrl(document.getCheminStockage());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(PRESIGNED_EXPIRY_MINUTES);
        return new DownloadUrlDTO(url, expiresAt);
    }

    @Transactional
    @Auditable(action = "UPDATE_STATUT_DOCUMENT")
    public DocumentDTO updateStatutControle(
            UUID documentId,
            StatutControleDTO dto,
            UUID organisationId
    ) {
        DocumentEntity document = getDocumentForOrganisation(documentId, organisationId);
        document.setStatutControle(dto.statut());
        if (dto.notes() != null) {
            document.setNotes(dto.notes());
        }
        return documentMapper.toDto(documentRepository.save(document));
    }

    @Transactional
    @Auditable(action = "DELETE_DOCUMENT")
    public void softDelete(UUID documentId, UUID organisationId) {
        DocumentEntity document = getDocumentForOrganisation(documentId, organisationId);
        document.setActif(false);
        documentRepository.save(document);
        minioStorageService.deleteFile(document.getCheminStockage());
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> listByDossier(UUID dossierId, UUID organisationId) {
        dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));

        return documentRepository.findByDossierIdAndOrganisationIdAndActifTrueOrderByCreatedAtDesc(
                        dossierId, organisationId)
                .stream()
                .map(documentMapper::toDto)
                .toList();
    }

    private DocumentEntity getDocumentForOrganisation(UUID id, UUID organisationId) {
        return documentRepository.findByIdAndOrganisationIdAndActifTrue(id, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND", "Document introuvable", HttpStatus.NOT_FOUND));
    }
}
