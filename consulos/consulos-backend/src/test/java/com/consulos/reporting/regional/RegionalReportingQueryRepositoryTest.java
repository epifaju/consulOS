package com.consulos.reporting.regional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalReportingQueryRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private RegionalReportingQueryRepository repository;

    private List<UUID> orgIds;
    private OffsetDateTime debut;
    private OffsetDateTime fin;

    @BeforeEach
    void setUp() {
        repository = new RegionalReportingQueryRepository();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        orgIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        debut = OffsetDateTime.parse("2026-09-01T00:00:00+02:00");
        fin = OffsetDateTime.parse("2026-10-01T00:00:00+02:00");
    }

    private void stubQuery() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void countDossiersCrees_whenOrgIdsNull_returnsZeroWithoutQuery() {
        assertThat(repository.countDossiersCrees(null, debut, fin)).isZero();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void countDossiersCrees_whenOrgIdsEmpty_returnsZeroWithoutQuery() {
        assertThat(repository.countDossiersCrees(Collections.emptyList(), debut, fin)).isZero();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void countDossiersCrees_returnsCount() {
        stubQuery();
        when(query.getSingleResult()).thenReturn(30L);

        long result = repository.countDossiersCrees(orgIds, debut, fin);

        assertThat(result).isEqualTo(30L);
        verify(query).setParameter("orgIds", orgIds);
    }

    @Test
    void avgDelaiHeures_whenOrgIdsEmpty_returnsZeroWithoutQuery() {
        assertThat(repository.avgDelaiHeures(Collections.emptyList(), debut, fin)).isEqualTo(BigDecimal.ZERO);
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void avgDelaiHeures_whenNull_returnsZero() {
        stubQuery();
        when(query.getSingleResult()).thenReturn(null);

        BigDecimal result = repository.avgDelaiHeures(orgIds, debut, fin);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void avgDelaiHeures_whenPresent_returnsValue() {
        stubQuery();
        when(query.getSingleResult()).thenReturn(6.0d);

        BigDecimal result = repository.avgDelaiHeures(orgIds, debut, fin);

        assertThat(result).isEqualByComparingTo("6.0");
    }

    @Test
    void dossiersCreesParStatut_whenOrgIdsNull_returnsEmptyWithoutQuery() {
        assertThat(repository.dossiersCreesParStatut(null, debut, fin)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void dossiersCreesParStatut_returnsRows() {
        stubQuery();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"SOUMIS", 4L},
                new Object[]{"VALIDE", 2L}
        ));

        List<Object[]> rows = repository.dossiersCreesParStatut(orgIds, debut, fin);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()[0]).isEqualTo("SOUMIS");
    }

    @Test
    void encaissementTotaux_whenOrgIdsEmpty_returnsZeroPairWithoutQuery() {
        Object[] result = repository.encaissementTotaux(Collections.emptyList(), debut, fin);

        assertThat(result).containsExactly(BigDecimal.ZERO, 0L);
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void encaissementTotaux_returnsRow() {
        stubQuery();
        Object[] totaux = new Object[]{new BigDecimal("900.00"), 12L};
        when(query.getSingleResult()).thenReturn(totaux);

        Object[] result = repository.encaissementTotaux(orgIds, debut, fin);

        assertThat(result).isSameAs(totaux);
    }

    @Test
    void encaissementsParMode_returnsRows() {
        stubQuery();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"VIREMENT", 3L, new BigDecimal("450.00")}
        ));

        List<Object[]> rows = repository.encaissementsParMode(orgIds, debut, fin);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[0]).isEqualTo("VIREMENT");
    }

    @Test
    void postesMensuel_whenOrgIdsEmpty_returnsEmptyWithoutQuery() {
        assertThat(repository.postesMensuel(Collections.emptyList(), debut, fin)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void postesMensuel_returnsRows() {
        stubQuery();
        UUID orgId = orgIds.getFirst();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{orgId, "POSTE-A", "Poste A", 5L, new BigDecimal("100.00"), 2L, 3L}
        ));

        List<Object[]> rows = repository.postesMensuel(orgIds, debut, fin);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[1]).isEqualTo("POSTE-A");
        assertThat(rows.getFirst()[3]).isEqualTo(5L);
    }

    @Test
    void rdvParStatut_whenOrgIdsNull_returnsEmptyWithoutQuery() {
        assertThat(repository.rdvParStatut(null, debut, fin)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }
}
