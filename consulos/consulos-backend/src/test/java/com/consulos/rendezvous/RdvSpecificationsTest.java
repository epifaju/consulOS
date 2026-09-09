package com.consulos.rendezvous;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class RdvSpecificationsTest {

    @Mock
    private Root<RendezvousEntity> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path path;
    @Mock
    private Predicate predicate;

    @Test
    void withStatut_null_shouldReturnNull() {
        assertThat(RdvSpecifications.withStatut(null)).isNull();
    }

    @Test
    void withStatut_present_shouldEqual() {
        Specification<RendezvousEntity> spec = RdvSpecifications.withStatut(RdvStatut.CONFIRME);
        assertThat(spec).isNotNull();

        when(root.get("statut")).thenReturn(path);
        when(cb.equal(path, RdvStatut.CONFIRME)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).equal(path, RdvStatut.CONFIRME);
    }

    @Test
    void withAgent_null_shouldReturnNull() {
        assertThat(RdvSpecifications.withAgent(null)).isNull();
    }

    @Test
    void withAgent_present_shouldEqual() {
        UUID agentId = UUID.randomUUID();
        Specification<RendezvousEntity> spec = RdvSpecifications.withAgent(agentId);
        assertThat(spec).isNotNull();

        when(root.get("agentId")).thenReturn(path);
        when(cb.equal(path, agentId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withUsager_null_shouldReturnNull() {
        assertThat(RdvSpecifications.withUsager(null)).isNull();
    }

    @Test
    void withUsager_present_shouldEqual() {
        UUID usagerId = UUID.randomUUID();
        Specification<RendezvousEntity> spec = RdvSpecifications.withUsager(usagerId);
        assertThat(spec).isNotNull();

        when(root.get("usagerId")).thenReturn(path);
        when(cb.equal(path, usagerId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withTypeService_null_shouldReturnNull() {
        assertThat(RdvSpecifications.withTypeService(null)).isNull();
    }

    @Test
    void withTypeService_present_shouldEqual() {
        UUID typeServiceId = UUID.randomUUID();
        Specification<RendezvousEntity> spec = RdvSpecifications.withTypeService(typeServiceId);
        assertThat(spec).isNotNull();

        when(root.get("typeServiceId")).thenReturn(path);
        when(cb.equal(path, typeServiceId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void forOrganisation_shouldEqual() {
        UUID organisationId = UUID.randomUUID();
        Specification<RendezvousEntity> spec = RdvSpecifications.forOrganisation(organisationId);
        assertThat(spec).isNotNull();

        when(root.get("organisationId")).thenReturn(path);
        when(cb.equal(path, organisationId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void betweenDates_bothNull_shouldReturnNull() {
        assertThat(RdvSpecifications.betweenDates(null, null)).isNull();
    }

    @Test
    void betweenDates_bothPresent_shouldBetween() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fin = OffsetDateTime.of(2026, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);
        Specification<RendezvousEntity> spec = RdvSpecifications.betweenDates(debut, fin);
        assertThat(spec).isNotNull();

        when(root.get("dateHeure")).thenReturn(path);
        when(cb.between(path, debut, fin)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).between(path, debut, fin);
    }

    @Test
    void betweenDates_debutOnly_shouldGreaterThanOrEqual() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<RendezvousEntity> spec = RdvSpecifications.betweenDates(debut, null);
        assertThat(spec).isNotNull();

        when(root.get("dateHeure")).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, debut)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).greaterThanOrEqualTo(path, debut);
    }

    @Test
    void betweenDates_finOnly_shouldLessThanOrEqual() {
        OffsetDateTime fin = OffsetDateTime.of(2026, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);
        Specification<RendezvousEntity> spec = RdvSpecifications.betweenDates(null, fin);
        assertThat(spec).isNotNull();

        when(root.get("dateHeure")).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, fin)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).lessThanOrEqualTo(path, fin);
    }
}
