package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.DossierStatut;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.encaissement.StatutEncaissement;
import com.consulos.rendezvous.RdvStatut;
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

import java.math.BigDecimal;
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
class UsagerActivityQueryServiceTest {

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
    void listActivity_usagerNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

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
    void listActivity_empty() {
        UsagerEntity usager = activeUsager();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of());
        when(relatedDataRepository.findRendezvousByUsager(organisationId, usagerId)).thenReturn(List.of());
        when(relatedDataRepository.findEncaissementsByUsager(organisationId, usagerId)).thenReturn(List.of());

        List<UsagerActivityItemDTO> result = service.listActivity(usagerId, organisationId);

        assertThat(result).isEmpty();
    }

    @Test
    void listActivity_mergesDossierRdvEncaissementSortedDesc() {
        UsagerEntity usager = activeUsager();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        UUID typeServiceId = UUID.randomUUID();
        UUID dossierId = UUID.randomUUID();
        UUID rdvId = UUID.randomUUID();
        UUID encId = UUID.randomUUID();

        LocalDateTime dossierCreated = LocalDateTime.of(2026, 9, 1, 10, 0);
        OffsetDateTime rdvAt = OffsetDateTime.parse("2026-09-05T14:00:00Z");
        OffsetDateTime encAt = OffsetDateTime.parse("2026-09-09T12:00:00Z");

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-100");
        dossier.setActif(true);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setStatut(DossierStatut.SOUMIS);
        dossier.setCreatedAt(dossierCreated);

        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setLibelle("Passeport");

        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(rdvId);
        rdv.setUsagerId(usagerId);
        rdv.setOrganisationId(organisationId);
        rdv.setDateHeure(rdvAt);
        rdv.setStatut(RdvStatut.CONFIRME);
        rdv.setTypeServiceId(typeServiceId);
        rdv.setMotif("Renouvellement");

        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(encId);
        enc.setNumeroRecu("REC-001");
        enc.setMontantTotal(new BigDecimal("45.00"));
        enc.setDevise("EUR");
        enc.setStatut(StatutEncaissement.PAYE);
        enc.setCreatedAt(encAt);

        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of(dossier));
        when(typeServiceRepository.findAllById(any())).thenReturn(List.of(type));
        when(relatedDataRepository.findRendezvousByUsager(organisationId, usagerId)).thenReturn(List.of(rdv));
        when(relatedDataRepository.findEncaissementsByUsager(organisationId, usagerId)).thenReturn(List.of(enc));

        List<UsagerActivityItemDTO> result = service.listActivity(usagerId, organisationId);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(UsagerActivityItemDTO::category)
                .containsExactly("ENCAISSEMENT", "RDV", "DOSSIER");
        assertThat(result.get(0).relatedId()).isEqualTo(encId);
        assertThat(result.get(0).title()).isEqualTo("REC-001");
        assertThat(result.get(1).relatedId()).isEqualTo(rdvId);
        assertThat(result.get(1).detail()).contains("CONFIRME").contains("Renouvellement");
        assertThat(result.get(2).relatedId()).isEqualTo(dossierId);
        assertThat(result.get(2).title()).isEqualTo("Dossier DOS-2026-100");
        assertThat(result.get(2).detail()).contains("Passeport").contains("SOUMIS");
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
