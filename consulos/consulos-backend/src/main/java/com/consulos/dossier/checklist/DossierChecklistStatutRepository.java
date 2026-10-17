package com.consulos.dossier.checklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DossierChecklistStatutRepository extends JpaRepository<DossierChecklistStatutEntity, UUID> {

    List<DossierChecklistStatutEntity> findByDossierIdAndOrganisationId(UUID dossierId, UUID organisationId);

    Optional<DossierChecklistStatutEntity> findByDossierIdAndOrganisationIdAndCode(
            UUID dossierId, UUID organisationId, String code);
}
