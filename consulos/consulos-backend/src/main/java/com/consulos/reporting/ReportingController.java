package com.consulos.reporting;

import com.consulos.common.ApiResponse;
import com.consulos.reporting.dto.ReportingMensuelDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
    private final ReportingExcelExportService excelExportService;
    private final ReportingCsvExportService csvExportService;

    @GetMapping("/mensuel")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ApiResponse<ReportingMensuelDTO> mensuel(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois
    ) {
        YearMonth ym = resolveYearMonth(annee, mois);
        UUID organisationId = ReportingSecurityUtils.getOrganisationId();
        return ApiResponse.ok(reportingService.mensuel(ym.getYear(), ym.getMonthValue(), organisationId));
    }

    @GetMapping("/mensuel/excel")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ResponseEntity<byte[]> mensuelExcel(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois
    ) {
        YearMonth ym = resolveYearMonth(annee, mois);
        UUID organisationId = ReportingSecurityUtils.getOrganisationId();
        byte[] xlsx = excelExportService.exportMensuel(ym.getYear(), ym.getMonthValue(), organisationId);
        String filename = "reporting-mensuel-%04d-%02d.xlsx".formatted(ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping("/dossiers/csv")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ResponseEntity<byte[]> dossiersCsv(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois
    ) {
        YearMonth ym = resolveYearMonth(annee, mois);
        UUID organisationId = ReportingSecurityUtils.getOrganisationId();
        byte[] csv = csvExportService.exportDossiersCsv(ym.getYear(), ym.getMonthValue(), organisationId);
        String filename = "dossiers-%04d-%02d.csv".formatted(ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/rdv/csv")
    @PreAuthorize("hasAnyRole('SUPERVISEUR','ADMIN')")
    public ResponseEntity<byte[]> rdvCsv(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois
    ) {
        YearMonth ym = resolveYearMonth(annee, mois);
        UUID organisationId = ReportingSecurityUtils.getOrganisationId();
        byte[] csv = csvExportService.exportRdvCsv(ym.getYear(), ym.getMonthValue(), organisationId);
        String filename = "rdv-%04d-%02d.csv".formatted(ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    private static YearMonth resolveYearMonth(Integer annee, Integer mois) {
        YearMonth now = YearMonth.now();
        int y = annee != null ? annee : now.getYear();
        int m = mois != null ? mois : now.getMonthValue();
        return YearMonth.of(y, m);
    }
}
