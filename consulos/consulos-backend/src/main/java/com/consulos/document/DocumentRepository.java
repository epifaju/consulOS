package com.consulos.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findByIdAndOrganisationIdAndActifTrue(UUID id, UUID organisationId);

    List<DocumentEntity> findByDossierIdAndOrganisationIdAndActifTrueOrderByCreatedAtDesc(
            UUID dossierId,
            UUID organisationId
    );
}
