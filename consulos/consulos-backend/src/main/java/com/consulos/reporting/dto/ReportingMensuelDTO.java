package com.consulos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportingMensuelDTO(
        int annee,
        int mois,
        LocalDate dateDebut,
        LocalDate dateFin,
        long dossiersCrees,
        List<StatutCountDTO> dossiersParStatut,
        EncaissementTotauxDTO encaissements,
        RdvTotauxDTO rendezVous,
        long documentsDeposes,
        BigDecimal delaiMoyenHeures
) {
}
