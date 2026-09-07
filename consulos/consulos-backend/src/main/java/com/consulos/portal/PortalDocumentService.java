package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentService;
import com.consulos.document.dto.DocumentDTO;
import com.consulos.portal.dto.PortalDocumentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PortalDocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "PIECE_IDENTITE", "JUSTIFICATIF", "PHOTO", "FORMULAIRE", "AUTRE"
    );

    private final DocumentService documentService;

    @Transactional(readOnly = true)
    public List<PortalDocumentDTO> list(PortalSession session) {
        return documentService.listByDossier(session.dossierId(), session.organisationId()).stream()
                .map(this::toPortalDto)
                .toList();
    }

    @Transactional
    public PortalDocumentDTO upload(PortalSession session, MultipartFile file, String typeDocument, String notes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_REQUIRED", "Fichier requis", HttpStatus.BAD_REQUEST);
        }
        String type = typeDocument != null ? typeDocument.trim().toUpperCase() : "AUTRE";
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException("TYPE_INVALID", "Type de document invalide", HttpStatus.BAD_REQUEST);
        }
        DocumentDTO created = documentService.upload(
                file,
                session.dossierId(),
                session.usagerId(),
                type,
                notes,
                null,
                session.organisationId()
        );
        return toPortalDto(created);
    }

    private PortalDocumentDTO toPortalDto(DocumentDTO dto) {
        return new PortalDocumentDTO(
                dto.id(),
                dto.nomOriginal(),
                dto.typeDocument(),
                dto.statutControle() != null ? dto.statutControle().name() : null,
                dto.tailleOctets() != null ? dto.tailleOctets() : 0L,
                dto.createdAt() != null ? dto.createdAt().toString() : null
        );
    }
}
