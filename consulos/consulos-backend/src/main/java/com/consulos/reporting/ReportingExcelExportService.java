package com.consulos.reporting;

import com.consulos.common.BusinessException;
import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.ModePaiementTotalDTO;
import com.consulos.reporting.dto.ReportingMensuelDTO;
import com.consulos.reporting.dto.ServiceTotalDTO;
import com.consulos.reporting.dto.StatutCountDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingExcelExportService {

    private final ReportingService reportingService;

    @Transactional(readOnly = true)
    public byte[] exportMensuel(int annee, int mois, UUID organisationId) {
        ReportingMensuelDTO report = reportingService.mensuel(annee, mois, organisationId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSynthese(workbook.createSheet("Synthese"), report);
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
                    "Impossible de générer le reporting Excel",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void writeSynthese(Sheet sheet, ReportingMensuelDTO report) {
        String[][] rows = {
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

    private void writeStatutSheet(Sheet sheet, java.util.List<StatutCountDTO> items, String col1, String col2) {
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
