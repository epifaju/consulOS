package com.consulos.document.query;

import com.consulos.document.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DocumentListQueryRepository
        extends JpaRepository<DocumentEntity, UUID>, JpaSpecificationExecutor<DocumentEntity> {
}
