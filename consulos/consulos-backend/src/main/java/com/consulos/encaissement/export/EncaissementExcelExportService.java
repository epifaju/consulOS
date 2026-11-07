package com.consulos.encaissement.export;

import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.encaissement.EncaissementDayBounds;
import com.consulos.encaissement.EncaissementService;
import com.consulos.encaissement.dto.EncaissementSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncaissementExcelExportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EncaissementService encaissementService;

    @Transactional(readOnly = true)
    public byte[] exportJournal(LocalDate dateDebut, LocalDate dateFin, UUID organisationId) {
        if (dateFin.isBefore(dateDebut)) {
            throw new BusinessException(
                    "INVALID_DATE_RANGE",
                    "La date de fin doit être postérieure ou égale à la date de début",
                    HttpStatus.BAD_REQUEST
            );
        }

        OffsetDateTime debut = EncaissementDayBounds.startOfDay(dateDebut);
        OffsetDateTime fin = EncaissementDayBounds.startOfNextDay(dateFin);

        PageResponse<EncaissementSummaryDTO> page = encaissementService.list(
                organisationId,
                null,
                null,
                null,
                debut,
                fin,
                Pageable.unpaged()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Journal caisse");
            Row header = sheet.createRow(0);
            String[] columns = {
                    "N° reçu", "Date", "Usager", "Dossier", "Service", "Montant", "Perçu", "Devise", "Mode", "Statut"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (EncaissementSummaryDTO enc : page.content()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(enc.numeroRecu() != null ? enc.numeroRecu() : "");
                row.createCell(1).setCellValue(enc.createdAt() != null ? enc.createdAt().format(DT) : "");
                String usager = ((enc.usagerPrenom() != null ? enc.usagerPrenom() : "") + " "
                        + (enc.usagerNom() != null ? enc.usagerNom() : "")).trim();
                row.createCell(2).setCellValue(usager);
                row.createCell(3).setCellValue(enc.dossierReference() != null ? enc.dossierReference() : "");
                row.createCell(4).setCellValue(enc.typeServiceLibelle() != null ? enc.typeServiceLibelle() : "");
                row.createCell(5).setCellValue(enc.montantTotal() != null ? enc.montantTotal().doubleValue() : 0);
                row.createCell(6).setCellValue(enc.montantPercu() != null ? enc.montantPercu().doubleValue() : 0);
                row.createCell(7).setCellValue(enc.devise() != null ? enc.devise() : "EUR");
                row.createCell(8).setCellValue(enc.modePaiement() != null ? enc.modePaiement().name() : "");
                row.createCell(9).setCellValue(enc.statut() != null ? enc.statut().name() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException(
                    "EXCEL_EXPORT_FAILED",
                    "Impossible de générer l'export Excel",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
