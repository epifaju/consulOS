package com.consulos.usager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsagerRepository extends JpaRepository<UsagerEntity, UUID> {

    Page<UsagerEntity> findByOrganisationIdAndActifTrue(UUID orgId, Pageable pageable);

    @Query(value = """
            SELECT * FROM usagers
            WHERE organisation_id = :orgId
              AND actif = true
              AND to_tsvector('french',
                    coalesce(nom,'') || ' ' || coalesce(prenom,'') || ' ' ||
                    coalesce(numero_document,'') || ' ' || coalesce(email,'') || ' ' ||
                    coalesce(telephone,'')
                  ) @@ plainto_tsquery('french', :query)
            ORDER BY nom, prenom
            LIMIT 20
            """, nativeQuery = true)
    List<UsagerEntity> searchFullText(@Param("orgId") UUID orgId, @Param("query") String query);

    @Query("""
            SELECT u FROM UsagerEntity u
            WHERE u.organisationId = :orgId
              AND u.actif = true
              AND LOWER(u.nom) = LOWER(:nom)
              AND u.dateNaissance = :dateNaissance
              AND u.nationalite = :nationalite
              AND u.id <> :excludeId
            """)
    List<UsagerEntity> findPotentialDoublons(
            @Param("orgId") UUID orgId,
            @Param("nom") String nom,
            @Param("dateNaissance") LocalDate dateNaissance,
            @Param("nationalite") String nationalite,
            @Param("excludeId") UUID excludeId
    );

    Optional<UsagerEntity> findByNumeroAndOrganisationId(String numero, UUID orgId);

    @Query(value = "SELECT nextval('usager_seq')", nativeQuery = true)
    Long nextUsagerSequence();
}
