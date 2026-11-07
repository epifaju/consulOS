package com.consulos.encaissement;

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
class EncaissementSpecificationsTest {

    @Mock
    private Root<EncaissementEntity> root;
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
        assertThat(EncaissementSpecifications.withStatut(null)).isNull();
    }

    @Test
    void withStatut_present_shouldEqual() {
        Specification<EncaissementEntity> spec = EncaissementSpecifications.withStatut(StatutEncaissement.PAYE);
        assertThat(spec).isNotNull();

        when(root.get("statut")).thenReturn(path);
        when(cb.equal(path, StatutEncaissement.PAYE)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).equal(path, StatutEncaissement.PAYE);
    }

    @Test
    void withModePaiement_null_shouldReturnNull() {
        assertThat(EncaissementSpecifications.withModePaiement(null)).isNull();
    }

    @Test
    void withModePaiement_present_shouldEqual() {
        Specification<EncaissementEntity> spec = EncaissementSpecifications.withModePaiement(ModePaiement.CARTE);
        assertThat(spec).isNotNull();

        when(root.get("modePaiement")).thenReturn(path);
        when(cb.equal(path, ModePaiement.CARTE)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withCaissier_null_shouldReturnNull() {
        assertThat(EncaissementSpecifications.withCaissier(null)).isNull();
    }

    @Test
    void withCaissier_present_shouldEqual() {
        UUID caissierId = UUID.randomUUID();
        Specification<EncaissementEntity> spec = EncaissementSpecifications.withCaissier(caissierId);
        assertThat(spec).isNotNull();

        when(root.get("caissierId")).thenReturn(path);
        when(cb.equal(path, caissierId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void forOrganisation_shouldEqual() {
        UUID organisationId = UUID.randomUUID();
        Specification<EncaissementEntity> spec = EncaissementSpecifications.forOrganisation(organisationId);
        assertThat(spec).isNotNull();

        when(root.get("organisationId")).thenReturn(path);
        when(cb.equal(path, organisationId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void betweenDates_bothNull_shouldReturnNull() {
        assertThat(EncaissementSpecifications.betweenDates(null, null)).isNull();
    }

    @Test
    void betweenDates_bothPresent_shouldBetweenOnCreatedAt() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fin = OffsetDateTime.of(2026, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);
        Specification<EncaissementEntity> spec = EncaissementSpecifications.betweenDates(debut, fin);
        assertThat(spec).isNotNull();

        when(root.get("createdAt")).thenReturn(path);
        when(cb.between(path, EncaissementDayBounds.toLocal(debut), EncaissementDayBounds.toLocal(fin)))
                .thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).between(path, EncaissementDayBounds.toLocal(debut), EncaissementDayBounds.toLocal(fin));
    }

    @Test
    void betweenDates_debutOnly_shouldGreaterThanOrEqual() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<EncaissementEntity> spec = EncaissementSpecifications.betweenDates(debut, null);
        assertThat(spec).isNotNull();

        when(root.get("createdAt")).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, EncaissementDayBounds.toLocal(debut))).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).greaterThanOrEqualTo(path, EncaissementDayBounds.toLocal(debut));
    }

    @Test
    void betweenDates_finOnly_shouldLessThanOrEqual() {
        OffsetDateTime fin = OffsetDateTime.of(2026, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);
        Specification<EncaissementEntity> spec = EncaissementSpecifications.betweenDates(null, fin);
        assertThat(spec).isNotNull();

        when(root.get("createdAt")).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, EncaissementDayBounds.toLocal(fin))).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).lessThanOrEqualTo(path, EncaissementDayBounds.toLocal(fin));
    }
}
