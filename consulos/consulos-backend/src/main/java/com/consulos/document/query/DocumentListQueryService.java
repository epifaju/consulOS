package com.consulos.document.query;

import com.consulos.common.PageResponse;
import com.consulos.document.DocumentEntity;
import com.consulos.document.StatutControle;
import com.consulos.document.query.dto.DocumentListItemDTO;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentListQueryService {

    private final DocumentListQueryRepository documentListQueryRepository;
    private final DossierRepository dossierRepository;
    private final UsagerRepository usagerRepository;

    @Transactional(readOnly = true)
    public PageResponse<DocumentListItemDTO> list(
            UUID organisationId,
            String typeDocument,
            List<StatutControle> statutControle,
            String search,
            String dossierReference,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            Pageable pageable
    ) {
        Specification<DocumentEntity> spec = Specification.where(
                        DocumentSpecifications.forOrganisation(organisationId))
                .and(DocumentSpecifications.withTypeDocument(typeDocument))
                .and(DocumentSpecifications.withStatuts(statutControle))
                .and(DocumentSpecifications.withSearch(search))
                .and(DocumentSpecifications.withDossierReference(dossierReference, organisationId))
                .and(DocumentSpecifications.withCreatedBetween(dateDebut, dateFin));

        Page<DocumentEntity> page = documentListQueryRepository.findAll(spec, pageable);
        Map<UUID, DossierEntity> dossiers = loadDossiers(page.getContent(), organisationId);
        Map<UUID, UsagerEntity> usagers = loadUsagers(page.getContent(), dossiers, organisationId);

        List<DocumentListItemDTO> content = page.getContent().stream()
                .map(doc -> toListItem(doc, dossiers, usagers))
                .toList();

        return new PageResponse<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    private DocumentListItemDTO toListItem(
            DocumentEntity doc,
            Map<UUID, DossierEntity> dossiers,
            Map<UUID, UsagerEntity> usagers
    ) {
        DossierEntity dossier = doc.getDossierId() != null ? dossiers.get(doc.getDossierId()) : null;
        UUID usagerId = doc.getUsagerId() != null ? doc.getUsagerId()
                : dossier != null ? dossier.getUsagerId() : null;
        UsagerEntity usager = usagerId != null ? usagers.get(usagerId) : null;

        return new DocumentListItemDTO(
                doc.getId(),
                doc.getDossierId(),
                dossier != null ? dossier.getReference() : null,
                usagerId,
                usager != null ? usager.getNom() : null,
                usager != null ? usager.getPrenom() : null,
                doc.getNomFichier(),
                doc.getNomOriginal(),
                doc.getTypeDocument(),
                doc.getMimeType(),
                doc.getTailleOctets(),
                doc.getStatutControle(),
                doc.getVersion(),
                doc.getNotes(),
                doc.getCreatedAt()
        );
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

    private Map<UUID, UsagerEntity> loadUsagers(
            List<DocumentEntity> documents,
            Map<UUID, DossierEntity> dossiers,
            UUID organisationId
    ) {
        Set<UUID> ids = new HashSet<>();
        for (DocumentEntity doc : documents) {
            if (doc.getUsagerId() != null) {
                ids.add(doc.getUsagerId());
            } else if (doc.getDossierId() != null) {
                DossierEntity dossier = dossiers.get(doc.getDossierId());
                if (dossier != null && dossier.getUsagerId() != null) {
                    ids.add(dossier.getUsagerId());
                }
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UsagerEntity> result = new HashMap<>();
        usagerRepository.findAllById(ids).stream()
                .filter(u -> organisationId.equals(u.getOrganisationId()) && Boolean.TRUE.equals(u.getActif()))
                .forEach(u -> result.put(u.getId(), u));
        return result;
    }
}
