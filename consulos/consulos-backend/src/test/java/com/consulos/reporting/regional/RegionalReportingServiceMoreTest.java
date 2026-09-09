package com.consulos.reporting.regional;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.common.BusinessException;
import com.consulos.reporting.regional.dto.RegionalReportingMensuelDTO;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalReportingServiceMoreTest {

    @Mock
    private RegionalReportingQueryRepository queryRepository;
    @Mock
    private ReseauService reseauService;

    @InjectMocks
    private RegionalReportingService regionalReportingService;

    private UUID userId;
    private UUID orgA;
    private UUID orgB;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
        ConsulosUserDetails details = new ConsulosUserDetails(
                userId, "chef@consulos.local", "hash", orgA, "ROLE_ADMIN", true
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
    void mensuel_whenInvalidYear_shouldThrow() {
        assertThatThrownBy(() -> regionalReportingService.mensuel(1999, 9, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("INVALID_YEAR");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void mensuel_whenReseauIdNull_shouldUseAllAllowedOrgs() {
        Set<UUID> allOrgs = Set.of(orgA, orgB);
        when(reseauService.resolveAllAllowedOrganisationIds(userId)).thenReturn(allOrgs);
        stubEmptyQueries(allOrgs);
        when(queryRepository.encaissementTotaux(eq(allOrgs), any(), any()))
                .thenReturn(new Object[]{new BigDecimal("10.00"), 1L});
        when(queryRepository.avgDelaiHeures(eq(allOrgs), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        RegionalReportingMensuelDTO result = regionalReportingService.mensuel(2026, 9, null);

        assertThat(result.reseauId()).isNull();
        assertThat(result.postesCount()).isEqualTo(2);
        verify(reseauService).resolveAllAllowedOrganisationIds(userId);
    }

    @Test
    void mensuel_whenEncaissementTotauxNullAmount_shouldUseZero() {
        Set<UUID> orgs = Set.of(orgA);
        UUID reseauId = UUID.randomUUID();
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(orgs);
        stubEmptyQueries(orgs);
        when(queryRepository.encaissementTotaux(eq(orgs), any(), any()))
                .thenReturn(new Object[]{null, 0L});
        when(queryRepository.avgDelaiHeures(eq(orgs), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        RegionalReportingMensuelDTO result = regionalReportingService.mensuel(2026, 9, reseauId);

        assertThat(result.encaissements().montantPercuTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.encaissements().nbTransactions()).isEqualTo(0L);
    }

    private void stubEmptyQueries(Set<UUID> orgIds) {
        when(queryRepository.dossiersCreesParStatut(eq(orgIds), any(), any())).thenReturn(List.of());
        when(queryRepository.countDossiersCrees(eq(orgIds), any(), any())).thenReturn(0L);
        when(queryRepository.encaissementsParMode(eq(orgIds), any(), any())).thenReturn(List.of());
        when(queryRepository.encaissementsParService(eq(orgIds), any(), any())).thenReturn(List.of());
        when(queryRepository.rdvParStatut(eq(orgIds), any(), any())).thenReturn(List.of());
        when(queryRepository.countDocuments(eq(orgIds), any(), any())).thenReturn(0L);
        when(queryRepository.postesMensuel(eq(orgIds), any(), any())).thenReturn(List.of());
    }
}
