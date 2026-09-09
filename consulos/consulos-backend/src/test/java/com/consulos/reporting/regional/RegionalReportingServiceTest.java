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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalReportingServiceTest {

    @Mock
    private RegionalReportingQueryRepository queryRepository;
    @Mock
    private ReseauService reseauService;

    @InjectMocks
    private RegionalReportingService regionalReportingService;

    private UUID userId;
    private UUID reseauId;
    private UUID orgA;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reseauId = UUID.randomUUID();
        orgA = UUID.randomUUID();
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
    void mensuel_whenInvalidMonth_shouldThrow() {
        assertThatThrownBy(() -> regionalReportingService.mensuel(2026, 13, reseauId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("INVALID_MONTH");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void mensuel_shouldAggregateScopedOrganisations() {
        OffsetDateTime debut = LocalDate.of(2026, 9, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = LocalDate.of(2026, 10, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));
        when(queryRepository.dossiersCreesParStatut(eq(Set.of(orgA)), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"SOUMIS", 4L}));
        when(queryRepository.countDossiersCrees(eq(Set.of(orgA)), any(), any())).thenReturn(4L);
        when(queryRepository.encaissementTotaux(eq(Set.of(orgA)), any(), any()))
                .thenReturn(new Object[]{new BigDecimal("120.00"), 3L});
        when(queryRepository.encaissementsParMode(eq(Set.of(orgA)), any(), any())).thenReturn(List.of());
        when(queryRepository.encaissementsParService(eq(Set.of(orgA)), any(), any())).thenReturn(List.of());
        when(queryRepository.rdvParStatut(eq(Set.of(orgA)), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"CONFIRME", 2L}));
        when(queryRepository.countDocuments(eq(Set.of(orgA)), any(), any())).thenReturn(8L);
        when(queryRepository.avgDelaiHeures(eq(Set.of(orgA)), any(), any()))
                .thenReturn(new BigDecimal("12.345"));
        when(queryRepository.postesMensuel(eq(Set.of(orgA)), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{orgA, "GNB-FR", "Paris", 4L, new BigDecimal("120.00"), 2L, 8L}
        ));

        RegionalReportingMensuelDTO result = regionalReportingService.mensuel(2026, 9, reseauId);

        assertThat(result.reseauId()).isEqualTo(reseauId);
        assertThat(result.postesCount()).isEqualTo(1);
        assertThat(result.dossiersCrees()).isEqualTo(4L);
        assertThat(result.encaissements().montantPercuTotal()).isEqualByComparingTo("120.00");
        assertThat(result.rendezVous().total()).isEqualTo(2L);
        assertThat(result.documentsDeposes()).isEqualTo(8L);
        assertThat(result.delaiMoyenHeures()).isEqualByComparingTo("12.35");
        assertThat(result.parPoste()).hasSize(1);
        assertThat(debut).isBefore(fin);
    }
}
