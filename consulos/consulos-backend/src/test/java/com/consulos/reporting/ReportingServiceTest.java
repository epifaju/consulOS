package com.consulos.reporting;

import com.consulos.common.BusinessException;
import com.consulos.reporting.dto.ReportingMensuelDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportingQueryRepository queryRepository;

    @InjectMocks
    private ReportingService reportingService;

    private UUID organisationId;
    private OffsetDateTime debut;
    private OffsetDateTime fin;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        debut = LocalDate.of(2026, 9, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        fin = LocalDate.of(2026, 10, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    @Test
    void resolvePeriod_shouldReturnMonthBounds() {
        ReportingService.PeriodBounds period = ReportingService.resolvePeriod(2026, 9);

        assertThat(period.dateDebut()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(period.dateFin()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(period.debut()).isEqualTo(debut);
        assertThat(period.fin()).isEqualTo(fin);
    }

    @Test
    void resolvePeriod_whenInvalidMonth_shouldThrow() {
        assertThatThrownBy(() -> ReportingService.resolvePeriod(2026, 0))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("INVALID_MONTH");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void resolvePeriod_whenInvalidYear_shouldThrow() {
        assertThatThrownBy(() -> ReportingService.resolvePeriod(1999, 6))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("INVALID_YEAR"));
    }

    @Test
    void mensuel_shouldAggregateDossiersEncaissementsRdvAndDocuments() {
        when(queryRepository.dossiersCreesParStatut(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"BROUILLON", 2L},
                new Object[]{"SOUMIS", 5L}
        ));
        when(queryRepository.countDossiersCrees(organisationId, debut, fin)).thenReturn(7L);
        when(queryRepository.encaissementTotaux(organisationId, debut, fin))
                .thenReturn(new Object[]{new BigDecimal("430.50"), 12L});
        when(queryRepository.encaissementsParMode(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"ESPECES", 8L, new BigDecimal("200.00")},
                new Object[]{"CARTE", 4L, new BigDecimal("230.50")}
        ));
        when(queryRepository.encaissementsParService(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"Passeport", new BigDecimal("300.00"), 6L}
        ));
        when(queryRepository.rdvParStatut(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"CONFIRME", 3L},
                new Object[]{"PRESENT", 2L}
        ));
        when(queryRepository.countDocuments(organisationId, debut, fin)).thenReturn(15L);
        when(queryRepository.avgDelaiHeures(organisationId, debut, fin)).thenReturn(new BigDecimal("36.456"));

        ReportingMensuelDTO result = reportingService.mensuel(2026, 9, organisationId);

        assertThat(result.annee()).isEqualTo(2026);
        assertThat(result.mois()).isEqualTo(9);
        assertThat(result.dossiersCrees()).isEqualTo(7L);
        assertThat(result.dossiersParStatut()).hasSize(2);
        assertThat(result.encaissements().montantPercuTotal()).isEqualByComparingTo("430.50");
        assertThat(result.encaissements().nbTransactions()).isEqualTo(12L);
        assertThat(result.encaissements().parMode()).hasSize(2);
        assertThat(result.encaissements().parService().getFirst().typeService()).isEqualTo("Passeport");
        assertThat(result.rendezVous().total()).isEqualTo(5L);
        assertThat(result.documentsDeposes()).isEqualTo(15L);
        assertThat(result.delaiMoyenHeures()).isEqualByComparingTo("36.46");
    }

    @Test
    void mensuel_whenEncaissementTotauxNull_shouldUseZero() {
        when(queryRepository.dossiersCreesParStatut(eq(organisationId), eq(debut), eq(fin))).thenReturn(List.of());
        when(queryRepository.countDossiersCrees(organisationId, debut, fin)).thenReturn(0L);
        when(queryRepository.encaissementTotaux(organisationId, debut, fin)).thenReturn(new Object[]{null, 0L});
        when(queryRepository.encaissementsParMode(organisationId, debut, fin)).thenReturn(List.of());
        when(queryRepository.encaissementsParService(organisationId, debut, fin)).thenReturn(List.of());
        when(queryRepository.rdvParStatut(organisationId, debut, fin)).thenReturn(List.of());
        when(queryRepository.countDocuments(organisationId, debut, fin)).thenReturn(0L);
        when(queryRepository.avgDelaiHeures(organisationId, debut, fin)).thenReturn(BigDecimal.ZERO);

        ReportingMensuelDTO result = reportingService.mensuel(2026, 9, organisationId);

        assertThat(result.encaissements().montantPercuTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.rendezVous().total()).isZero();
    }
}
