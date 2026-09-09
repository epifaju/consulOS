package com.consulos.reporting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingCsvExportServiceTest {

    @Mock
    private ReportingQueryRepository queryRepository;

    @InjectMocks
    private ReportingCsvExportService csvExportService;

    private UUID organisationId;
    private OffsetDateTime debut;
    private OffsetDateTime fin;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        debut = LocalDate.of(2026, 3, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        fin = LocalDate.of(2026, 4, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    @Test
    void exportDossiersCsv_shouldEscapeCommasAndQuotes() {
        when(queryRepository.listDossiersCsv(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"DOS-1", "SOUMIS", "Passeport, express", "2026-03-10", "Aminata", "Camara \"A\""}
        ));

        String csv = new String(csvExportService.exportDossiersCsv(2026, 3, organisationId), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("reference,statut,type_service,created_at,usager_prenom,usager_nom\n");
        assertThat(csv).contains("\"Passeport, express\"");
        assertThat(csv).contains("\"Camara \"\"A\"\"\"");
    }

    @Test
    void exportRdvCsv_shouldIncludeHeaderAndRows() {
        when(queryRepository.listRdvCsv(organisationId, debut, fin)).thenReturn(List.<Object[]>of(
                new Object[]{"2026-03-15T09:00", "CONFIRME", "Visa", "Mamadou", "Diallo", "Fatou", "Sow"}
        ));

        String csv = new String(csvExportService.exportRdvCsv(2026, 3, organisationId), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("date_heure,statut,type_service,usager_prenom,usager_nom,agent_prenom,agent_nom\n");
        assertThat(csv).contains("CONFIRME,Visa,Mamadou,Diallo,Fatou,Sow");
    }
}
