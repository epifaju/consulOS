package com.consulos.dashboard.regional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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
class RegionalAlertsQueryRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private RegionalAlertsQueryRepository repository;

    private List<UUID> orgIds;

    @BeforeEach
    void setUp() {
        repository = new RegionalAlertsQueryRepository();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        orgIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    private void stubQuery() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void dossiersBloquesEnRevue_whenOrgIdsNull_returnsEmptyWithoutQuery() {
        assertThat(repository.dossiersBloquesEnRevue(null)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void dossiersBloquesEnRevue_whenOrgIdsEmpty_returnsEmptyWithoutQuery() {
        assertThat(repository.dossiersBloquesEnRevue(Collections.emptyList())).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void dossiersBloquesEnRevue_mapsUuids() {
        stubQuery();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(id1, id2));

        List<UUID> ids = repository.dossiersBloquesEnRevue(orgIds);

        assertThat(ids).containsExactly(id1, id2);
        verify(query).setParameter("orgIds", orgIds);
    }

    @Test
    void dossiersEcheanceDepassee_whenOrgIdsEmpty_returnsEmptyWithoutQuery() {
        assertThat(repository.dossiersEcheanceDepassee(Collections.emptyList())).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void dossiersEcheanceDepassee_mapsUuids() {
        stubQuery();
        UUID id = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(id));

        List<UUID> ids = repository.dossiersEcheanceDepassee(orgIds);

        assertThat(ids).containsExactly(id);
    }

    @Test
    void agentsSurcharges_whenOrgIdsNull_returnsEmptyWithoutQuery() {
        assertThat(repository.agentsSurcharges(null)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void agentsSurcharges_returnsRows() {
        stubQuery();
        UUID orgId = orgIds.getFirst();
        UUID agentId = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{orgId, agentId, 22L}
        ));

        List<Object[]> rows = repository.agentsSurcharges(orgIds);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[0]).isEqualTo(orgId);
        assertThat(rows.getFirst()[1]).isEqualTo(agentId);
        assertThat(rows.getFirst()[2]).isEqualTo(22L);
    }

    @Test
    void postesCaisseSilencieuse_whenOrgIdsEmpty_returnsEmptyWithoutQuery() {
        assertThat(repository.postesCaisseSilencieuse(Collections.emptyList(), LocalDate.of(2026, 9, 9)))
                .isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void postesCaisseSilencieuse_returnsRows() {
        stubQuery();
        UUID orgId = orgIds.getFirst();
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{orgId, "POSTE-C"}
        ));

        List<Object[]> rows = repository.postesCaisseSilencieuse(orgIds, LocalDate.of(2026, 9, 9));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()[1]).isEqualTo("POSTE-C");
    }

    @Test
    void posteCodesForDossierIds_whenEmpty_returnsEmptyWithoutQuery() {
        assertThat(repository.posteCodesForDossierIds(Collections.emptyList())).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void posteCodesForDossierIds_whenNull_returnsEmptyWithoutQuery() {
        assertThat(repository.posteCodesForDossierIds(null)).isEmpty();
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void posteCodesForDossierIds_mapsCodes() {
        stubQuery();
        List<UUID> dossierIds = List.of(UUID.randomUUID());
        when(query.getResultList()).thenReturn(List.of("POSTE-A", "POSTE-B"));

        List<String> codes = repository.posteCodesForDossierIds(dossierIds);

        assertThat(codes).containsExactly("POSTE-A", "POSTE-B");
        verify(query).setParameter("dossierIds", dossierIds);
    }
}
