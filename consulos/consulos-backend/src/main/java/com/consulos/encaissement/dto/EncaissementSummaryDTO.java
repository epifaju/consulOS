package com.consulos.encaissement.dto;

import com.consulos.encaissement.ModePaiement;
import com.consulos.encaissement.StatutEncaissement;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EncaissementSummaryDTO(
        UUID id,
        String numeroRecu,
        UUID usagerId,
        String usagerNom,
        String usagerPrenom,
        UUID dossierId,
        String dossierReference,
        String typeServiceLibelle,
        BigDecimal montantTotal,
        BigDecimal montantPercu,
        String devise,
        ModePaiement modePaiement,
        StatutEncaissement statut,
        OffsetDateTime createdAt
) {
}
