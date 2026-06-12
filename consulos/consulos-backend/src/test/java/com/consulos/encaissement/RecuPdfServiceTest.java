package com.consulos.encaissement;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.usager.UsagerEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecuPdfServiceTest {

    private final RecuPdfService recuPdfService = new RecuPdfService();

    @Test
    void generateRecu_shouldReturnValidPdf() {
        UUID orgId = UUID.randomUUID();
        OrganisationEntity org = new OrganisationEntity();
        org.setId(orgId);
        org.setNom("Consulat Général de Guinée-Bissau");
        org.setCode("GNB-FR");

        UsagerEntity usager = new UsagerEntity();
        usager.setId(UUID.randomUUID());
        usager.setOrganisationId(orgId);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        usager.setNumero("USG-2026-00001");

        DossierEntity dossier = new DossierEntity();
        dossier.setId(UUID.randomUUID());
        dossier.setReference("DOS-2026-00042");
        dossier.setTypeServiceId(UUID.randomUUID());

        TypeServiceEntity typeService = new TypeServiceEntity();
        typeService.setLibelle("Passeport ordinaire");

        UserEntity caissier = new UserEntity();
        caissier.setNom("Diallo");
        caissier.setPrenom("Mamadou");

        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(UUID.randomUUID());
        enc.setOrganisationId(orgId);
        enc.setNumeroRecu("REC-2026-00001");
        enc.setMontantTotal(new BigDecimal("86.00"));
        enc.setMontantPercu(new BigDecimal("86.00"));
        enc.setDevise("EUR");
        enc.setModePaiement(ModePaiement.ESPECES);
        enc.setStatut(StatutEncaissement.PAYE);
        enc.setCreatedAt(OffsetDateTime.now());

        byte[] pdf = recuPdfService.generateRecu(enc, usager, dossier, typeService, org, caissier);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
