package com.consulos.dossier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DossierHistoriqueRepository extends JpaRepository<DossierHistoriqueEntity, UUID> {

    List<DossierHistoriqueEntity> findByDossierIdOrderByCreatedAtAsc(UUID dossierId);
}
