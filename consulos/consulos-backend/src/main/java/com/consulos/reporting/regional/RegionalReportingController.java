package com.consulos.reporting.regional;

import com.consulos.common.ApiResponse;
import com.consulos.reporting.regional.dto.RegionalReportingMensuelDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@RequestMapping("/regional/reporting")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
@Tag(name = "15 — Réseau multi-postes", description = "Reporting multi-sites (Vague C)")
public class RegionalReportingController {

    private final RegionalReportingService regionalReportingService;
    private final RegionalReportingExcelExportService excelExportService;

    @GetMapping("/mensuel")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Reporting mensuel agrégé multi-postes")
    public ApiResponse<RegionalReportingMensuelDTO> mensuel(
            @RequestParam(required = false) UUID reseauId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois
    ) {
        YearMonth ym = resolveYearMonth(annee, mois);
        return ApiResponse.ok(regionalReportingService.mensuel(ym.getYear(), ym.getMonthValue(), reseauId));
    }

    @GetMapping("/mensuel/excel")
    @PreAuthorize("hasAnyRole('ADMIN_RESEAU','ADMIN','SUPERVISEUR')")
    @Operation(summary = "Export Excel reporting mensuel multi-postes")
    public ResponseEntity<byte[]> mensuelExcel(
            @RequestParam(required = false) UUID reseauId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois
    ) {
        YearMonth ym = resolveYearMonth(annee, mois);
        byte[] xlsx = excelExportService.exportMensuel(ym.getYear(), ym.getMonthValue(), reseauId);
        String filename = "reporting-regional-%04d-%02d.xlsx".formatted(ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    private static YearMonth resolveYearMonth(Integer annee, Integer mois) {
        YearMonth now = YearMonth.now();
        int y = annee != null ? annee : now.getYear();
        int m = mois != null ? mois : now.getMonthValue();
        return YearMonth.of(y, m);
    }
}
