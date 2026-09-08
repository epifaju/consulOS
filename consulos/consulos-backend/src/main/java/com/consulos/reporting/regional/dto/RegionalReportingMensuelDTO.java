package com.consulos.reporting.regional.dto;

import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.RdvTotauxDTO;
import com.consulos.reporting.dto.StatutCountDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegionalReportingMensuelDTO(
        UUID reseauId,
        int postesCount,
        int annee,
        int mois,
        LocalDate dateDebut,
        LocalDate dateFin,
        long dossiersCrees,
        List<StatutCountDTO> dossiersParStatut,
        EncaissementTotauxDTO encaissements,
        RdvTotauxDTO rendezVous,
        long documentsDeposes,
        BigDecimal delaiMoyenHeures,
        List<RegionalPosteMensuelDTO> parPoste
) {
}
