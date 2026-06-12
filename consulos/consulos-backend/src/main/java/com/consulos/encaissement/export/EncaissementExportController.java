package com.consulos.encaissement.export;

import com.consulos.encaissement.EncaissementSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Encaissements — Exports", description = "Exports Excel du journal de caisse")
@RestController
@RequestMapping("/encaissements")
@RequiredArgsConstructor
public class EncaissementExportController {

    private final EncaissementExcelExportService excelExportService;

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('CAISSIER','SUPERVISEUR','ADMIN')")
    @Operation(summary = "Exporter le journal de caisse au format Excel (.xlsx)")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        UUID organisationId = EncaissementSecurityUtils.getOrganisationId();
        LocalDate effectiveFin = dateFin != null ? dateFin : dateDebut;
        byte[] xlsx = excelExportService.exportJournal(dateDebut, effectiveFin, organisationId);
        String filename = "journal-caisse-" + dateDebut + "_" + effectiveFin + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
