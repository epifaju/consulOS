package com.consulos.reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingCsvExportService {

    private final ReportingQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public byte[] exportDossiersCsv(int annee, int mois, UUID organisationId) {
        ReportingService.PeriodBounds period = ReportingService.resolvePeriod(annee, mois);
        List<Object[]> rows = queryRepository.listDossiersCsv(organisationId, period.debut(), period.fin());
        StringBuilder csv = new StringBuilder();
        csv.append("reference,statut,type_service,created_at,usager_prenom,usager_nom\n");
        for (Object[] row : rows) {
            csv.append(escapeCsv(str(row[0]))).append(',');
            csv.append(escapeCsv(str(row[1]))).append(',');
            csv.append(escapeCsv(str(row[2]))).append(',');
            csv.append(escapeCsv(str(row[3]))).append(',');
            csv.append(escapeCsv(str(row[4]))).append(',');
            csv.append(escapeCsv(str(row[5]))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportRdvCsv(int annee, int mois, UUID organisationId) {
        ReportingService.PeriodBounds period = ReportingService.resolvePeriod(annee, mois);
        List<Object[]> rows = queryRepository.listRdvCsv(organisationId, period.debut(), period.fin());
        StringBuilder csv = new StringBuilder();
        csv.append("date_heure,statut,type_service,usager_prenom,usager_nom,agent_prenom,agent_nom\n");
        for (Object[] row : rows) {
            csv.append(escapeCsv(str(row[0]))).append(',');
            csv.append(escapeCsv(str(row[1]))).append(',');
            csv.append(escapeCsv(str(row[2]))).append(',');
            csv.append(escapeCsv(str(row[3]))).append(',');
            csv.append(escapeCsv(str(row[4]))).append(',');
            csv.append(escapeCsv(str(row[5]))).append(',');
            csv.append(escapeCsv(str(row[6]))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
