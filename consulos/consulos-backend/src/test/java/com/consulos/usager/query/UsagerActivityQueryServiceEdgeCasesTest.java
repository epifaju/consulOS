package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.DossierStatut;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.rendezvous.RendezvousEntity;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.query.dto.UsagerActivityItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsagerActivityQueryServiceEdgeCasesTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private UsagerActivityRelatedDataRepository relatedDataRepository;

    @InjectMocks
    private UsagerActivityQueryService service;

    private UUID organisationId;
    private UUID usagerId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
    }

    @Test
    void listActivity_whenWrongOrganisation_shouldThrowUsagerNotFound() {
        UsagerEntity usager = activeUsager();
        usager.setOrganisationId(UUID.randomUUID());
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> service.listActivity(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verifyNoInteractions(dossierRepository, relatedDataRepository, typeServiceRepository);
    }

    @Test
    void listActivity_whenUsagerInactive_shouldThrowUsagerNotFound() {
        UsagerEntity usager = activeUsager();
        usager.setActif(false);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> service.listActivity(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verifyNoInteractions(dossierRepository, relatedDataRepository, typeServiceRepository);
    }

    @Test
    void listActivity_whenTypeServiceMissing_shouldUsePrestationFallback() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));

        UUID typeServiceId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        UUID rdvId = UUID.randomUUID();
        UUID encId = UUID.randomUUID();

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-200");
        dossier.setActif(true);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setStatut(DossierStatut.BROUILLON);
        dossier.setCreatedAt(LocalDateTime.of(2026, 9, 1, 10, 0));

        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(rdvId);
        rdv.setUsagerId(usagerId);
        rdv.setOrganisationId(organisationId);
        rdv.setDateHeure(OffsetDateTime.parse("2026-09-05T14:00:00Z"));
        rdv.setStatut(null);
        rdv.setMotif(null);

        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(encId);
        enc.setNumeroRecu(null);
        enc.setMontantTotal(null);
        enc.setDevise(null);
        enc.setStatut(null);
        enc.setCreatedAt(OffsetDateTime.parse("2026-09-09T12:00:00Z"));

        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of(dossier));
        when(typeServiceRepository.findAllById(any())).thenReturn(List.of());
        when(relatedDataRepository.findRendezvousByUsager(organisationId, usagerId)).thenReturn(List.of(rdv));
        when(relatedDataRepository.findEncaissementsByUsager(organisationId, usagerId)).thenReturn(List.of(enc));

        List<UsagerActivityItemDTO> result = service.listActivity(usagerId, organisationId);

        assertThat(result).hasSize(3);
        UsagerActivityItemDTO dossierItem = result.stream()
                .filter(i -> "DOSSIER".equals(i.category()))
                .findFirst()
                .orElseThrow();
        assertThat(dossierItem.detail()).startsWith("Prestation — ");
        assertThat(dossierItem.detail()).contains("BROUILLON");

        UsagerActivityItemDTO rdvItem = result.stream()
                .filter(i -> "RDV".equals(i.category()))
                .findFirst()
                .orElseThrow();
        assertThat(rdvItem.detail()).isEqualTo("PLANIFIE");

        UsagerActivityItemDTO encItem = result.stream()
                .filter(i -> "ENCAISSEMENT".equals(i.category()))
                .findFirst()
                .orElseThrow();
        assertThat(encItem.title()).isEqualTo("Encaissement");
        assertThat(encItem.detail()).isEqualTo("— EUR — ");
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Ibrahima");
        usager.setEmail("ibrahima@example.com");
        return usager;
    }
}
