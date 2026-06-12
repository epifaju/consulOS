package com.consulos.usager.rgpd.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UsagerRgpdExportDTO(
        OffsetDateTime exportedAt,
        String exportVersion,
        UsagerRgpdProfileDTO usager,
        List<UsagerRgpdDossierDTO> dossiers,
        List<UsagerRgpdRendezvousDTO> rendezvous,
        List<UsagerRgpdEncaissementDTO> encaissements,
        List<UsagerRgpdDocumentDTO> documents
) {

    public record UsagerRgpdProfileDTO(
            UUID id,
            String numero,
            String nom,
            String prenom,
            String dateNaissance,
            String lieuNaissance,
            String nationalite,
            String typeUsager,
            String telephone,
            String email,
            String adresse,
            String numeroDocument,
            String typeDocument,
            Map<String, Object> metadata,
            boolean actif,
            String createdAt,
            String updatedAt
    ) {
    }

    public record UsagerRgpdDossierDTO(
            UUID id,
            String reference,
            String statut,
            String priorite,
            String typeServiceLibelle,
            String dateSoumission,
            String dateEcheance,
            String createdAt
    ) {
    }

    public record UsagerRgpdRendezvousDTO(
            UUID id,
            String dateHeure,
            String statut,
            String motif,
            Integer dureeMinutes,
            String typeServiceLibelle
    ) {
    }

    public record UsagerRgpdEncaissementDTO(
            UUID id,
            String numeroRecu,
            String montantTotal,
            String montantPercu,
            String devise,
            String statut,
            String modePaiement,
            String createdAt
    ) {
    }

    public record UsagerRgpdDocumentDTO(
            UUID id,
            String nomFichier,
            String typeMime,
            Long tailleOctets,
            String categorie,
            String createdAt
    ) {
    }
}
