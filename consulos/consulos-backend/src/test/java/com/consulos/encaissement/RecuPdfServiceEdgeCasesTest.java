package com.consulos.encaissement;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.common.BusinessException;
import com.consulos.usager.UsagerEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecuPdfServiceEdgeCasesTest {

    private final RecuPdfService recuPdfService = new RecuPdfService();

    @ParameterizedTest
    @EnumSource(StatutEncaissement.class)
    void generateRecu_withNullOptionals_shouldCoverAllStatutColors(StatutEncaissement statut) {
        OrganisationEntity org = new OrganisationEntity();
        org.setNom("Consulat test");

        UsagerEntity usager = new UsagerEntity();
        usager.setNom("Diallo");
        usager.setPrenom("Awa");
        usager.setNumero("USG-1");

        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(UUID.randomUUID());
        enc.setNumeroRecu("REC-" + statut.name());
        enc.setMontantTotal(new BigDecimal("50.00"));
        enc.setMontantPercu(null);
        enc.setDevise(null);
        enc.setModePaiement(null);
        enc.setStatut(statut);
        enc.setCreatedAt(null);

        byte[] pdf = recuPdfService.generateRecu(enc, usager, null, null, org, null);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateRecu_whenOrganisationNomNull_shouldThrowPdfGenerationFailed() {
        OrganisationEntity org = new OrganisationEntity();
        org.setNom(null);

        UsagerEntity usager = new UsagerEntity();
        usager.setNom("X");
        usager.setPrenom("Y");
        usager.setNumero("USG-2");

        EncaissementEntity enc = new EncaissementEntity();
        enc.setNumeroRecu("REC-FAIL");
        enc.setMontantTotal(BigDecimal.TEN);
        enc.setStatut(StatutEncaissement.PAYE);

        assertThatThrownBy(() -> recuPdfService.generateRecu(enc, usager, null, null, org, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("PDF_GENERATION_ECHOUEE"));
    }
}
