package com.consulos.dossier.assign;

import com.consulos.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DossierAssignAgentRepository extends JpaRepository<UserEntity, UUID> {

    @Query("""
            SELECT u FROM UserEntity u
            JOIN FETCH u.role r
            WHERE u.organisationId = :organisationId
              AND u.actif = true
              AND r.code IN :roleCodes
            ORDER BY u.nom ASC, u.prenom ASC
            """)
    List<UserEntity> findAssignableAgents(
            @Param("organisationId") UUID organisationId,
            @Param("roleCodes") Collection<String> roleCodes
    );
}
