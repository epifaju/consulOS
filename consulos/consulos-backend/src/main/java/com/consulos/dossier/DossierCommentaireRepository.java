package com.consulos.dossier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DossierCommentaireRepository extends JpaRepository<DossierCommentaireEntity, UUID> {

    List<DossierCommentaireEntity> findByDossierIdOrderByCreatedAtDesc(UUID dossierId);
}
