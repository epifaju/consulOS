package com.consulos.document.storage;

import com.consulos.document.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentStorageReconciliationRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByOrganisationIdAndActifTrue(UUID organisationId);
}
