package com.consulos.reporting;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingQueryRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private ReportingQueryRepository repository;

    private UUID organisationId;
    private OffsetDateTime debut;
    private OffsetDateTime fin;

    @BeforeEach
    void setUp() {
        repository = new ReportingQueryRepository();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        organisationId = UUID.randomUUID();
        debut = OffsetDateTime.parse("2026-09-01T00:00:00+02:00");
        fin = OffsetDateTime.parse("2026-10-01T00:00:00+02:00");
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void countDossiersCrees_returnsCount() {
        when(query.getSingleResult()).thenReturn(42);

        long result = repository.countDossiersCrees(organisationId, debut, fin);

        assertThat(result).isEqualTo(42L);
        verify(query).setParameter("orgId", organisationId);
        verify(query).setParameter("debut", debut);
        verify(query).setParameter("fin", fin);
    }

    @Test
    void avgDelaiHeures_whenNull_returnsZero() {
        when(query.getSingleResult()).thenReturn(null);

        BigDecimal result = repository.avgDelaiHeures(organisationId, debut, fin);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void avgDelaiHeures_whenPresent_returnsValue() {
        when(query.getSingleResult()).thenReturn(new BigDecimal("8.25"));

        BigDecimal result = repository.avgDelaiHeures(organisationId, debut, fin);

        assertThat(result).isEqualByComparingTo("8.25");
    }

    @Test
    void dossiersCreesParStatut_returnsRows() {
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"EN_REVUE", 10L},
                new Object[]{"CLOTURE", 3L}
        ));

        List<Object[]> rows = repository.dossiersCreesParStatut(organisationId, debut, fin);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()[0]).isEqualTo("EN_REVUE");
        assertThat(rows.getFirst()[1]).isEqualTo(10L);
    }

    @Test
    void encaissementsParMode_returnsRows() {
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"ESPECES", 5L, new BigDecimal("200.00")},
                new Object[]{"CARTE", 2L, new BigDecimal("80.00")}
        ));

        List<Object[]> rows = repository.encaissementsParMode(organisationId, debut, fin);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()[0]).isEqualTo("ESPECES");
        assertThat(rows.getFirst()[2]).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    void encaissementTotaux_returnsRow() {
        Object[] totaux = new Object[]{new BigDecimal("500.00"), 7L};
        when(query.getSingleResult()).thenReturn(totaux);

        Object[] result = repository.encaissementTotaux(organisationId, debut, fin);

        assertThat(result).isSameAs(totaux);
        assertThat(result[0]).isEqualTo(new BigDecimal("500.00"));
        assertThat(result[1]).isEqualTo(7L);
    }

    @Test
    void rdvParStatut_returnsRows() {
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 8L}
        ));

        List<Object[]> rows = repository.rdvParStatut(organisationId, debut, fin);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[1]).isEqualTo(8L);
    }

    @Test
    void countDocuments_returnsCount() {
        when(query.getSingleResult()).thenReturn(15L);

        long result = repository.countDocuments(organisationId, debut, fin);

        assertThat(result).isEqualTo(15L);
    }
}
