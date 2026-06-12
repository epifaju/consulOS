package com.consulos.dossier;

import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.dto.DossierCreateDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {

    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private DossierHistoriqueRepository historiqueRepository;
    @Mock
    private DossierCommentaireRepository commentaireRepository;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkflowEngine workflowEngine;

    @InjectMocks
    private DossierService dossierService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID organisationId;
    private UUID usagerId;
    private UUID typeServiceId;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void create_shouldGenerateReferenceAndBrouillonStatus() throws Exception {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");

        TypeServiceEntity typeService = new TypeServiceEntity();
        typeService.setId(typeServiceId);
        typeService.setOrganisationId(organisationId);
        typeService.setActif(true);
        typeService.setCode("PASSEPORT");
        typeService.setLibelle("Passeport ordinaire");
        typeService.setDelaiJours(30);
        typeService.setWorkflowConfig(objectMapper.readTree("{\"transitions\":{}}"));
        typeService.setChecklist(objectMapper.readTree("[]"));

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));
        when(dossierRepository.nextDossierSequence()).thenReturn(42L);
        when(dossierRepository.save(any(DossierEntity.class))).thenAnswer(invocation -> {
            DossierEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        var result = dossierService.create(
                new DossierCreateDTO(usagerId, typeServiceId, null, null),
                organisationId,
                userId
        );

        ArgumentCaptor<DossierEntity> dossierCaptor = ArgumentCaptor.forClass(DossierEntity.class);
        verify(dossierRepository).save(dossierCaptor.capture());
        DossierEntity saved = dossierCaptor.getValue();

        assertThat(saved.getReference()).matches("DOS-\\d{4}-00042");
        assertThat(saved.getStatut()).isEqualTo(DossierStatut.BROUILLON);
        assertThat(result.reference()).matches("DOS-\\d{4}-00042");
        assertThat(result.statut()).isEqualTo(DossierStatut.BROUILLON);

        ArgumentCaptor<DossierHistoriqueEntity> historiqueCaptor = ArgumentCaptor.forClass(DossierHistoriqueEntity.class);
        verify(historiqueRepository).save(historiqueCaptor.capture());
        assertThat(historiqueCaptor.getValue().getStatutDe()).isNull();
        assertThat(historiqueCaptor.getValue().getStatutVers()).isEqualTo("BROUILLON");
    }

    @Test
    void transition_nominal_shouldCreateHistorique() throws Exception {
        UUID dossierId = UUID.randomUUID();

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setActif(true);
        dossier.setStatut(DossierStatut.BROUILLON);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setReference("DOS-2026-00001");

        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Diallo");
        usager.setPrenom("Fatou");

        TypeServiceEntity typeService = new TypeServiceEntity();
        typeService.setId(typeServiceId);
        typeService.setOrganisationId(organisationId);
        typeService.setActif(true);
        typeService.setCode("PASSEPORT");
        typeService.setLibelle("Passeport ordinaire");
        typeService.setWorkflowConfig(objectMapper.readTree("{\"transitions\":{\"BROUILLON\":[\"SOUMIS\"]}}"));

        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(dossierRepository.save(any(DossierEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dossierService.transition(
                dossierId,
                DossierStatut.SOUMIS,
                null,
                userId,
                "ROLE_AGENT",
                organisationId
        );

        ArgumentCaptor<DossierHistoriqueEntity> historiqueCaptor = ArgumentCaptor.forClass(DossierHistoriqueEntity.class);
        verify(historiqueRepository).save(historiqueCaptor.capture());
        assertThat(historiqueCaptor.getValue().getStatutDe()).isEqualTo("BROUILLON");
        assertThat(historiqueCaptor.getValue().getStatutVers()).isEqualTo("SOUMIS");
    }

    @Test
    void transition_invalid_shouldPropagateWorkflowException() throws Exception {
        UUID dossierId = UUID.randomUUID();

        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setActif(true);
        dossier.setStatut(DossierStatut.EN_REVUE);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);

        TypeServiceEntity typeService = new TypeServiceEntity();
        typeService.setId(typeServiceId);
        typeService.setOrganisationId(organisationId);
        typeService.setActif(true);
        typeService.setWorkflowConfig(objectMapper.readTree("{\"transitions\":{}}"));

        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));

        doThrow(new BusinessException("TRANSITION_INVALIDE", "Transition refusée", org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(workflowEngine).validateTransition(
                        eq(DossierStatut.EN_REVUE),
                        eq(DossierStatut.VALIDE),
                        any(),
                        eq("ROLE_AGENT"),
                        eq(null)
                );

        assertThatThrownBy(() -> dossierService.transition(
                dossierId,
                DossierStatut.VALIDE,
                null,
                userId,
                "ROLE_AGENT",
                organisationId
        )).isInstanceOf(BusinessException.class);
    }
}
