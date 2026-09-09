package com.consulos.dashboard;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardQueryRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private DashboardQueryRepository repository;

    private UUID organisationId;

    @BeforeEach
    void setUp() {
        repository = new DashboardQueryRepository();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        organisationId = UUID.randomUUID();
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void countDossiersByStatuts_returnsCount() {
        when(query.getSingleResult()).thenReturn(12L);

        long result = repository.countDossiersByStatuts(organisationId, List.of("EN_REVUE", "SOUMIS"));

        assertThat(result).isEqualTo(12L);
        verify(entityManager).createNativeQuery(anyString());
        verify(query).setParameter("orgId", organisationId);
        verify(query).setParameter("statuts", List.of("EN_REVUE", "SOUMIS"));
    }

    @Test
    void avgDelaiHeures_whenNull_returnsZero() {
        when(query.getSingleResult()).thenReturn(null);

        BigDecimal result = repository.avgDelaiHeures(
                organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7));

        assertThat(result).isEqualTo(BigDecimal.ZERO);
        verify(entityManager).createNativeQuery(anyString());
    }

    @Test
    void avgDelaiHeures_whenPresent_returnsValue() {
        when(query.getSingleResult()).thenReturn(4.5d);

        BigDecimal result = repository.avgDelaiHeures(
                organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7));

        assertThat(result).isEqualByComparingTo("4.5");
    }

    @Test
    void dossiersParJour_returnsRows() {
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{LocalDate.of(2026, 9, 1), 3L},
                new Object[]{LocalDate.of(2026, 9, 2), 5L}
        ));

        List<Object[]> rows = repository.dossiersParJour(
                organisationId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7));

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()[1]).isEqualTo(3L);
        assertThat(rows.get(1)[1]).isEqualTo(5L);
    }

    @Test
    void rdvStatsForDay_returnsRows() {
        OffsetDateTime debut = OffsetDateTime.parse("2026-09-09T00:00:00+02:00");
        OffsetDateTime fin = OffsetDateTime.parse("2026-09-10T00:00:00+02:00");
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 4L},
                new Object[]{"ANNULE", 1L}
        ));

        List<Object[]> rows = repository.rdvStatsForDay(organisationId, debut, fin);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()[0]).isEqualTo("CONFIRME");
        verify(query).setParameter("debut", debut);
        verify(query).setParameter("fin", fin);
    }

    @Test
    void dossiersBloquesEnRevue_mapsUuids() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(id1, id2));

        List<UUID> ids = repository.dossiersBloquesEnRevue(organisationId);

        assertThat(ids).containsExactly(id1, id2);
    }

    @Test
    void sumRecettesJour_returnsAmount() {
        when(query.getSingleResult()).thenReturn(new BigDecimal("150.00"));

        BigDecimal result = repository.sumRecettesJour(organisationId, LocalDate.of(2026, 9, 9));

        assertThat(result).isEqualByComparingTo("150.00");
    }

    @Test
    void agentsSurcharges_returnsRows() {
        UUID agentId = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{agentId, 25L}
        ));

        List<Object[]> rows = repository.agentsSurcharges(organisationId);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[0]).isEqualTo(agentId);
        assertThat(rows.getFirst()[1]).isEqualTo(25L);
    }

    @Test
    void refreshMaterializedView_executesUpdate() {
        when(query.executeUpdate()).thenReturn(0);

        repository.refreshMaterializedView();

        verify(entityManager).createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY dashboard_daily_stats");
        verify(query).executeUpdate();
    }
}
