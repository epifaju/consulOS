package com.consulos.reporting.regional;

import com.consulos.common.BusinessException;
import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.ModePaiementTotalDTO;
import com.consulos.reporting.dto.ServiceTotalDTO;
import com.consulos.reporting.dto.StatutCountDTO;
import com.consulos.reporting.regional.dto.RegionalPosteMensuelDTO;
import com.consulos.reporting.regional.dto.RegionalReportingMensuelDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalReportingExcelExportService {

    private final RegionalReportingService regionalReportingService;

    @Transactional(readOnly = true)
    public byte[] exportMensuel(int annee, int mois, UUID reseauId) {
        RegionalReportingMensuelDTO report = regionalReportingService.mensuel(annee, mois, reseauId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSynthese(workbook.createSheet("Synthese"), report);
            writePostes(workbook.createSheet("Postes"), report.parPoste());
            writeStatutSheet(workbook.createSheet("Dossiers"), report.dossiersParStatut(), "Statut", "Nombre");
            writeEncaissements(workbook.createSheet("Encaissements"), report.encaissements());
            writeStatutSheet(workbook.createSheet("RDV"), report.rendezVous().parStatut(), "Statut", "Nombre");

            workbook.write(out);
            return out.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    "EXCEL_EXPORT_FAILED",
                    "Impossible de générer le reporting régional Excel",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void writeSynthese(Sheet sheet, RegionalReportingMensuelDTO report) {
        String[][] rows = {
                {"Réseau ID", report.reseauId() != null ? report.reseauId().toString() : "ALL"},
                {"Postes", String.valueOf(report.postesCount())},
                {"Année", String.valueOf(report.annee())},
                {"Mois", String.valueOf(report.mois())},
                {"Période début", report.dateDebut().toString()},
                {"Période fin", report.dateFin().toString()},
                {"Dossiers créés", String.valueOf(report.dossiersCrees())},
                {"Documents déposés", String.valueOf(report.documentsDeposes())},
                {"RDV total", String.valueOf(report.rendezVous().total())},
                {"Encaissements (nb)", String.valueOf(report.encaissements().nbTransactions())},
                {"Montant perçu", report.encaissements().montantPercuTotal().toPlainString()},
                {"Délai moyen (h)", report.delaiMoyenHeures().toPlainString()},
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writePostes(Sheet sheet, List<RegionalPosteMensuelDTO> postes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Code");
        header.createCell(1).setCellValue("Poste");
        header.createCell(2).setCellValue("Dossiers créés");
        header.createCell(3).setCellValue("Encaissements (nb)");
        header.createCell(4).setCellValue("Montant perçu");
        header.createCell(5).setCellValue("RDV");

        int idx = 1;
        for (RegionalPosteMensuelDTO poste : postes) {
            Row row = sheet.createRow(idx++);
            row.createCell(0).setCellValue(poste.code());
            row.createCell(1).setCellValue(poste.nom());
            row.createCell(2).setCellValue(poste.dossiersCrees());
            row.createCell(3).setCellValue(poste.nbEncaissements());
            row.createCell(4).setCellValue(poste.montantPercu().doubleValue());
            row.createCell(5).setCellValue(poste.rdvTotal());
        }
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeStatutSheet(Sheet sheet, List<StatutCountDTO> items, String col1, String col2) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue(col1);
        header.createCell(1).setCellValue(col2);
        int idx = 1;
        for (StatutCountDTO item : items) {
            Row row = sheet.createRow(idx++);
            row.createCell(0).setCellValue(item.statut());
            row.createCell(1).setCellValue(item.count());
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeEncaissements(Sheet sheet, EncaissementTotauxDTO enc) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Dimension");
        header.createCell(1).setCellValue("Clé");
        header.createCell(2).setCellValue("Nombre");
        header.createCell(3).setCellValue("Montant");

        int idx = 1;
        for (ModePaiementTotalDTO mode : enc.parMode()) {
            Row row = sheet.createRow(idx++);
            row.createCell(0).setCellValue("Mode");
            row.createCell(1).setCellValue(mode.mode());
            row.createCell(2).setCellValue(mode.count());
            row.createCell(3).setCellValue(mode.montant().doubleValue());
        }
        for (ServiceTotalDTO service : enc.parService()) {
            Row row = sheet.createRow(idx++);
            row.createCell(0).setCellValue("Service");
            row.createCell(1).setCellValue(service.typeService());
            row.createCell(2).setCellValue(service.nbTransactions());
            row.createCell(3).setCellValue(service.montantTotal().doubleValue());
        }
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
