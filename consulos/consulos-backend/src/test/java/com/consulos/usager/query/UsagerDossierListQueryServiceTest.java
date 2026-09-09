package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.DossierStatut;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.dto.DossierSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsagerDossierListQueryServiceTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;

    @InjectMocks
    private UsagerDossierListQueryService service;

    private UUID organisationId;
    private UUID usagerId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
    }

    @Test
    void listDossiers_usagerNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listDossiers(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verifyNoInteractions(dossierRepository, typeServiceRepository);
    }

    @Test
    void listDossiers_happy_numeroFromReference_typeServiceLibelle() {
        UUID dossierId = UUID.randomUUID();
        UUID typeServiceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 9, 9, 30);

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-777");
        dossier.setActif(true);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setStatut(DossierStatut.EN_REVUE);
        dossier.setCreatedAt(createdAt);

        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setLibelle("Visa");

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of(dossier));
        when(typeServiceRepository.findAllById(any())).thenReturn(List.of(type));

        List<DossierSummaryDTO> result = service.listDossiers(usagerId, organisationId);

        assertThat(result).hasSize(1);
        DossierSummaryDTO summary = result.getFirst();
        assertThat(summary.id()).isEqualTo(dossierId);
        assertThat(summary.numero()).isEqualTo("DOS-2026-777");
        assertThat(summary.typeService()).isEqualTo("Visa");
        assertThat(summary.statut()).isEqualTo("EN_REVUE");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void listDossiers_missingType_typeServiceNull() {
        UUID dossierId = UUID.randomUUID();
        UUID typeServiceId = UUID.randomUUID();

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-888");
        dossier.setActif(true);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setStatut(DossierStatut.BROUILLON);
        dossier.setCreatedAt(LocalDateTime.of(2026, 9, 8, 8, 0));

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of(dossier));
        when(typeServiceRepository.findAllById(any())).thenReturn(List.of());

        List<DossierSummaryDTO> result = service.listDossiers(usagerId, organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numero()).isEqualTo("DOS-2026-888");
        assertThat(result.getFirst().typeService()).isNull();
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Bah");
        usager.setPrenom("Mariama");
        usager.setEmail("mariama@example.com");
        return usager;
    }
}
