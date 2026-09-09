package com.consulos.reporting.regional;

import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.ModePaiementTotalDTO;
import com.consulos.reporting.dto.RdvTotauxDTO;
import com.consulos.reporting.dto.ServiceTotalDTO;
import com.consulos.reporting.dto.StatutCountDTO;
import com.consulos.reporting.regional.dto.RegionalPosteMensuelDTO;
import com.consulos.reporting.regional.dto.RegionalReportingMensuelDTO;
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
class RegionalReportingExcelExportServiceTest {

    @Mock
    private RegionalReportingService regionalReportingService;

    @InjectMocks
    private RegionalReportingExcelExportService exportService;

    @Test
    void exportMensuel_shouldReturnXlsxBytes() {
        UUID reseauId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        RegionalReportingMensuelDTO report = new RegionalReportingMensuelDTO(
                reseauId,
                1,
                2026,
                9,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                20L,
                List.of(new StatutCountDTO("EN_REVUE", 4L)),
                new EncaissementTotauxDTO(
                        new BigDecimal("1000.00"),
                        25L,
                        List.of(new ModePaiementTotalDTO("CARTE", 10L, new BigDecimal("500.00"))),
                        List.of(new ServiceTotalDTO("Visa", new BigDecimal("400.00"), 8L))
                ),
                new RdvTotauxDTO(30L, List.of(new StatutCountDTO("PRESENT", 12L))),
                40L,
                new BigDecimal("24.00"),
                List.of(new RegionalPosteMensuelDTO(
                        orgId,
                        "GNB-FR",
                        "Consulat Paris",
                        20L,
                        new BigDecimal("1000.00"),
                        25L,
                        30L
                ))
        );
        when(regionalReportingService.mensuel(2026, 9, reseauId)).thenReturn(report);

        byte[] bytes = exportService.exportMensuel(2026, 9, reseauId);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }
}
