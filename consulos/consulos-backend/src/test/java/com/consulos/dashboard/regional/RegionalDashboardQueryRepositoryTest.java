package com.consulos.dashboard.regional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class RegionalDashboardQueryRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private RegionalDashboardQueryRepository repository;

    private List<UUID> orgIds;

    @BeforeEach
    void setUp() {
        repository = new RegionalDashboardQueryRepository();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        orgIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    private void stubQuery() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void countDossiersByStatuts_whenOrgIdsNull_returnsZeroWithoutQuery() {
        assertThat(repository.countDossiersByStatuts(null, List.of("EN_REVUE"))).isZero();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void countDossiersByStatuts_whenOrgIdsEmpty_returnsZeroWithoutQuery() {
        assertThat(repository.countDossiersByStatuts(Collections.emptyList(), List.of("EN_REVUE"))).isZero();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void countDossiersByStatuts_returnsCount() {
        stubQuery();
        when(query.getSingleResult()).thenReturn(18);

        long result = repository.countDossiersByStatuts(orgIds, List.of("EN_REVUE", "SOUMIS"));

        assertThat(result).isEqualTo(18L);
        verify(query).setParameter("orgIds", orgIds);
        verify(query).setParameter("statuts", List.of("EN_REVUE", "SOUMIS"));
    }

    @Test
    void countDossiersEnRetard_whenOrgIdsEmpty_returnsZeroWithoutQuery() {
        assertThat(repository.countDossiersEnRetard(Collections.emptyList())).isZero();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void countDossiersEnRetard_returnsCount() {
        stubQuery();
        when(query.getSingleResult()).thenReturn(7L);

        assertThat(repository.countDossiersEnRetard(orgIds)).isEqualTo(7L);
    }

    @Test
    void rdvStatsForDay_whenOrgIdsNull_returnsEmptyWithoutQuery() {
        OffsetDateTime debut = OffsetDateTime.parse("2026-09-09T00:00:00+02:00");
        OffsetDateTime fin = OffsetDateTime.parse("2026-09-10T00:00:00+02:00");

        assertThat(repository.rdvStatsForDay(null, debut, fin)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void rdvStatsForDay_returnsRows() {
        stubQuery();
        OffsetDateTime debut = OffsetDateTime.parse("2026-09-09T00:00:00+02:00");
        OffsetDateTime fin = OffsetDateTime.parse("2026-09-10T00:00:00+02:00");
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 6L},
                new Object[]{"NO_SHOW", 1L}
        ));

        List<Object[]> rows = repository.rdvStatsForDay(orgIds, debut, fin);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()[0]).isEqualTo("CONFIRME");
        verify(query).setParameter("debut", debut);
        verify(query).setParameter("fin", fin);
    }

    @Test
    void sumRecettesJour_whenOrgIdsEmpty_returnsZeroWithoutQuery() {
        assertThat(repository.sumRecettesJour(Collections.emptyList(), LocalDate.of(2026, 9, 9)))
                .isEqualTo(BigDecimal.ZERO);
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void sumRecettesJour_returnsAmount() {
        stubQuery();
        when(query.getSingleResult()).thenReturn(new BigDecimal("250.50"));

        BigDecimal result = repository.sumRecettesJour(orgIds, LocalDate.of(2026, 9, 9));

        assertThat(result).isEqualByComparingTo("250.50");
    }

    @Test
    void sumRecettesPeriode_whenOrgIdsNull_returnsZeroWithoutQuery() {
        assertThat(repository.sumRecettesPeriode(null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7)))
                .isEqualTo(BigDecimal.ZERO);
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void sumRecettesPeriode_returnsAmount() {
        stubQuery();
        when(query.getSingleResult()).thenReturn("99.00");

        BigDecimal result = repository.sumRecettesPeriode(
                orgIds, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7));

        assertThat(result).isEqualByComparingTo("99.00");
    }

    @Test
    void postesKpis_whenOrgIdsEmpty_returnsEmptyWithoutQuery() {
        OffsetDateTime rdvDebut = OffsetDateTime.parse("2026-09-09T00:00:00+02:00");
        OffsetDateTime rdvFin = OffsetDateTime.parse("2026-09-10T00:00:00+02:00");

        assertThat(repository.postesKpis(Collections.emptyList(), rdvDebut, rdvFin, LocalDate.of(2026, 9, 9)))
                .isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void postesKpis_returnsRows() {
        stubQuery();
        OffsetDateTime rdvDebut = OffsetDateTime.parse("2026-09-09T00:00:00+02:00");
        OffsetDateTime rdvFin = OffsetDateTime.parse("2026-09-10T00:00:00+02:00");
        UUID orgId = orgIds.getFirst();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{orgId, "POSTE-B", "Poste B", 10L, 4L, 2L, new BigDecimal("75.00")}
        ));

        List<Object[]> rows = repository.postesKpis(orgIds, rdvDebut, rdvFin, LocalDate.of(2026, 9, 9));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[1]).isEqualTo("POSTE-B");
        assertThat(rows.getFirst()[3]).isEqualTo(10L);
        assertThat(rows.getFirst()[6]).isEqualTo(new BigDecimal("75.00"));
    }
}
