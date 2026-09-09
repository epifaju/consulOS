package com.consulos.dashboard.regional;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import com.consulos.dashboard.regional.dto.RegionalPosteKpiDTO;
import com.consulos.dashboard.regional.dto.RegionalStatsDTO;
import com.consulos.reseau.ReseauOrganisationLookupRepository;
import com.consulos.reseau.ReseauService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalDashboardServiceTest {

    @Mock
    private RegionalDashboardQueryRepository repository;
    @Mock
    private ReseauService reseauService;
    @Mock
    private ReseauOrganisationLookupRepository organisationLookupRepository;

    @InjectMocks
    private RegionalDashboardService regionalDashboardService;

    private UUID userId;
    private UUID reseauId;
    private UUID orgA;
    private UUID orgB;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reseauId = UUID.randomUUID();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();

        ConsulosUserDetails details = new ConsulosUserDetails(
                userId, "admin@consulos.local", "hash", orgA, "ROLE_ADMIN", true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStats_shouldAggregateAcrossAllowedOrganisations() {
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA, orgB));
        when(repository.countDossiersByStatuts(
                Set.of(orgA, orgB), List.of("BROUILLON", "SOUMIS", "INCOMPLET", "EN_REVUE", "VALIDE")
        )).thenReturn(20L);
        when(repository.countDossiersByStatuts(Set.of(orgA, orgB), List.of("EN_REVUE"))).thenReturn(3L);
        when(repository.countDossiersEnRetard(Set.of(orgA, orgB))).thenReturn(2L);
        when(repository.rdvStatsForDay(eq(Set.of(orgA, orgB)), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 4L},
                new Object[]{"PRESENT", 1L}
        ));
        when(repository.sumRecettesJour(eq(Set.of(orgA, orgB)), any())).thenReturn(new BigDecimal("150.00"));
        when(repository.sumRecettesPeriode(eq(Set.of(orgA, orgB)), any(), any()))
                .thenReturn(new BigDecimal("900.00"));

        RegionalStatsDTO stats = regionalDashboardService.getStats(
                reseauId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );

        assertThat(stats.reseauId()).isEqualTo(reseauId);
        assertThat(stats.postesCount()).isEqualTo(2);
        assertThat(stats.dossiersOuverts()).isEqualTo(20L);
        assertThat(stats.enRevue()).isEqualTo(3L);
        assertThat(stats.rdvJour()).isEqualTo(5L);
        assertThat(stats.dossiersEnRetard()).isEqualTo(2L);
        assertThat(stats.recettesJour()).isEqualByComparingTo("150.00");
        assertThat(stats.recettesPeriode()).isEqualByComparingTo("900.00");
    }

    @Test
    void getPosteStats_whenOrganisationOutsideScope_shouldThrow() {
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));

        assertThatThrownBy(() -> regionalDashboardService.getPosteStats(orgB, reseauId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("POSTE_NOT_IN_SCOPE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void listPostesKpis_shouldMapRepositoryRows() {
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));
        when(repository.postesKpis(eq(Set.of(orgA)), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{orgA, "GNB-FR", "Consulat Paris", 8L, 2L, 3L, new BigDecimal("75.00")}
        ));

        List<RegionalPosteKpiDTO> kpis = regionalDashboardService.listPostesKpis(reseauId);

        assertThat(kpis).hasSize(1);
        assertThat(kpis.getFirst().organisationId()).isEqualTo(orgA);
        assertThat(kpis.getFirst().code()).isEqualTo("GNB-FR");
        assertThat(kpis.getFirst().nom()).isEqualTo("Consulat Paris");
    }
}
