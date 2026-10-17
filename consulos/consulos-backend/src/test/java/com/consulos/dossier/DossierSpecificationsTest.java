package com.consulos.dossier;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class DossierSpecificationsTest {

    @Mock
    private Root<DossierEntity> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path organisationPath;
    @Mock
    private Path actifPath;
    @Mock
    private Path fieldPath;
    @Mock
    private Predicate predicate;
    @Mock
    private Predicate orgPredicate;
    @Mock
    private Predicate actifPredicate;

    @Test
    void forOrganisation_shouldFilterTenantAndActif() {
        UUID organisationId = UUID.randomUUID();
        Specification<DossierEntity> spec = DossierSpecifications.forOrganisation(organisationId);

        when(root.get("organisationId")).thenReturn(organisationPath);
        when(root.get("actif")).thenReturn(actifPath);
        when(cb.equal(organisationPath, organisationId)).thenReturn(orgPredicate);
        when(cb.equal(actifPath, true)).thenReturn(actifPredicate);
        when(cb.and(orgPredicate, actifPredicate)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).equal(organisationPath, organisationId);
        verify(cb).equal(actifPath, true);
    }

    @Test
    void withStatuts_nullOrEmpty_shouldReturnNull() {
        assertThat(DossierSpecifications.withStatuts(null)).isNull();
        assertThat(DossierSpecifications.withStatuts(List.of())).isNull();
    }

    @Test
    void withStatuts_present_shouldIn() {
        List<DossierStatut> statuts = List.of(DossierStatut.SOUMIS, DossierStatut.EN_REVUE);
        Specification<DossierEntity> spec = DossierSpecifications.withStatuts(statuts);
        when(root.get("statut")).thenReturn(fieldPath);
        when(fieldPath.in(statuts)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withTypeService_agent_usager_priorite_null_shouldReturnNull() {
        assertThat(DossierSpecifications.withTypeService(null)).isNull();
        assertThat(DossierSpecifications.withAgent(null)).isNull();
        assertThat(DossierSpecifications.withUsager(null)).isNull();
        assertThat(DossierSpecifications.withPriorite(null)).isNull();
    }

    @Test
    void withTypeService_present_shouldEqual() {
        UUID typeId = UUID.randomUUID();
        Specification<DossierEntity> spec = DossierSpecifications.withTypeService(typeId);
        when(root.get("typeServiceId")).thenReturn(fieldPath);
        when(cb.equal(fieldPath, typeId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withAgent_present_shouldEqual() {
        UUID agentId = UUID.randomUUID();
        Specification<DossierEntity> spec = DossierSpecifications.withAgent(agentId);
        when(root.get("agentId")).thenReturn(fieldPath);
        when(cb.equal(fieldPath, agentId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withUsager_present_shouldEqual() {
        UUID usagerId = UUID.randomUUID();
        Specification<DossierEntity> spec = DossierSpecifications.withUsager(usagerId);
        when(root.get("usagerId")).thenReturn(fieldPath);
        when(cb.equal(fieldPath, usagerId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withPriorite_present_shouldEqual() {
        Specification<DossierEntity> spec = DossierSpecifications.withPriorite(DossierPriorite.URGENTE);
        when(root.get("priorite")).thenReturn(fieldPath);
        when(cb.equal(fieldPath, DossierPriorite.URGENTE)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withCreatedBetween_bothNull_shouldReturnNull() {
        assertThat(DossierSpecifications.withCreatedBetween(null, null)).isNull();
    }

    @Test
    void withCreatedBetween_both_shouldBetweenLocalDateTime() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fin = OffsetDateTime.of(2026, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<DossierEntity> spec = DossierSpecifications.withCreatedBetween(debut, fin);
        when(root.get("createdAt")).thenReturn(fieldPath);
        when(cb.between(fieldPath, debut.toLocalDateTime(), fin.toLocalDateTime())).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withCreatedBetween_debutOnly_shouldGreaterOrEqual() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<DossierEntity> spec = DossierSpecifications.withCreatedBetween(debut, null);
        when(root.get("createdAt")).thenReturn(fieldPath);
        when(cb.greaterThanOrEqualTo(fieldPath, debut.toLocalDateTime())).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withCreatedBetween_finOnly_shouldLessOrEqual() {
        OffsetDateTime fin = OffsetDateTime.of(2026, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<DossierEntity> spec = DossierSpecifications.withCreatedBetween(null, fin);
        when(root.get("createdAt")).thenReturn(fieldPath);
        when(cb.lessThanOrEqualTo(fieldPath, fin.toLocalDateTime())).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }
}
