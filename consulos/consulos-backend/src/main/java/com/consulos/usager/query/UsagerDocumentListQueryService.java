package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.query.dto.UsagerDocumentSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsagerDocumentListQueryService {

    private final UsagerRepository usagerRepository;
    private final UsagerDocumentQueryRepository usagerDocumentQueryRepository;
    private final DossierRepository dossierRepository;

    @Transactional(readOnly = true)
    public List<UsagerDocumentSummaryDTO> listDocuments(UUID usagerId, UUID organisationId) {
        assertUsagerAccessible(usagerId, organisationId);

        List<DocumentEntity> documents = usagerDocumentQueryRepository.findByUsager(organisationId, usagerId);
        Map<UUID, DossierEntity> dossiers = loadDossiers(documents, organisationId);

        return documents.stream()
                .map(doc -> toSummary(doc, dossiers))
                .toList();
    }

    private void assertUsagerAccessible(UUID usagerId, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(usagerId)
                .orElseThrow(() -> new BusinessException(
                        "USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
    }

    private Map<UUID, DossierEntity> loadDossiers(List<DocumentEntity> documents, UUID organisationId) {
        Set<UUID> ids = new HashSet<>();
        for (DocumentEntity doc : documents) {
            if (doc.getDossierId() != null) {
                ids.add(doc.getDossierId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, DossierEntity> result = new HashMap<>();
        dossierRepository.findAllById(ids).stream()
                .filter(d -> organisationId.equals(d.getOrganisationId()) && Boolean.TRUE.equals(d.getActif()))
                .forEach(d -> result.put(d.getId(), d));
        return result;
    }

    private UsagerDocumentSummaryDTO toSummary(DocumentEntity doc, Map<UUID, DossierEntity> dossiers) {
        DossierEntity dossier = doc.getDossierId() != null ? dossiers.get(doc.getDossierId()) : null;
        return new UsagerDocumentSummaryDTO(
                doc.getId(),
                doc.getDossierId(),
                dossier != null ? dossier.getReference() : null,
                doc.getNomOriginal(),
                doc.getTypeDocument(),
                doc.getMimeType(),
                doc.getTailleOctets(),
                doc.getStatutControle(),
                doc.getCreatedAt()
        );
    }
}
