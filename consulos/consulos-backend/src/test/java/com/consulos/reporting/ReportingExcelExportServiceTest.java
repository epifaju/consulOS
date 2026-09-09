package com.consulos.reporting;

import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.ModePaiementTotalDTO;
import com.consulos.reporting.dto.RdvTotauxDTO;
import com.consulos.reporting.dto.ReportingMensuelDTO;
import com.consulos.reporting.dto.ServiceTotalDTO;
import com.consulos.reporting.dto.StatutCountDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingExcelExportServiceTest {

    @Mock
    private ReportingService reportingService;

    @InjectMocks
    private ReportingExcelExportService exportService;

    @Test
    void exportMensuel_shouldReturnXlsxBytes() {
        UUID organisationId = UUID.randomUUID();
        ReportingMensuelDTO report = new ReportingMensuelDTO(
                2026,
                9,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                12L,
                List.of(new StatutCountDTO("SOUMIS", 5L)),
                new EncaissementTotauxDTO(
                        new BigDecimal("430.50"),
                        12L,
                        List.of(new ModePaiementTotalDTO("ESPECES", 8L, new BigDecimal("200.00"))),
                        List.of(new ServiceTotalDTO("Passeport", new BigDecimal("300.00"), 6L))
                ),
                new RdvTotauxDTO(10L, List.of(new StatutCountDTO("CONFIRME", 7L))),
                15L,
                new BigDecimal("36.45")
        );
        when(reportingService.mensuel(2026, 9, organisationId)).thenReturn(report);

        byte[] bytes = exportService.exportMensuel(2026, 9, organisationId);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }
}
