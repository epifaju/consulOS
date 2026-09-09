package com.consulos.dossier;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.dto.DossierUpdateDTO;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Écarts non couverts par DossierServiceTest : assignation, update, rejet sans motif, stats.
 */
@ExtendWith(MockitoExtension.class)
class DossierServiceEdgeCasesTest {

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
    private UUID otherOrganisationId;
    private UUID dossierId;
    private UUID usagerId;
    private UUID typeServiceId;
    private UUID agentId;
    private UUID userId;
    private DossierEntity dossier;
    private UsagerEntity usager;
    private TypeServiceEntity typeService;
    private UserEntity agent;

    @BeforeEach
    void setUp() throws Exception {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setReference("DOS-2026-00010");
        dossier.setStatut(DossierStatut.EN_REVUE);
        dossier.setPriorite(DossierPriorite.NORMALE);
        dossier.setActif(true);

        usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Mamadou");

        typeService = new TypeServiceEntity();
        typeService.setId(typeServiceId);
        typeService.setOrganisationId(organisationId);
        typeService.setActif(true);
        typeService.setCode("PASSEPORT");
        typeService.setLibelle("Passeport");
        typeService.setWorkflowConfig(objectMapper.readTree("{\"transitions\":{}}"));
        typeService.setChecklist(objectMapper.readTree("[]"));

        agent = new UserEntity();
        agent.setId(agentId);
        agent.setOrganisationId(organisationId);
        agent.setActif(true);
        agent.setNom("Diallo");
        agent.setPrenom("Fatou");
    }

    @Test
    void assignerAgent_shouldSetAgentOnDossier() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(dossierRepository.save(any(DossierEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));
        when(commentaireRepository.findByDossierIdOrderByCreatedAtDesc(dossierId)).thenReturn(List.of());

        var result = dossierService.assignerAgent(dossierId, agentId, organisationId);

        assertThat(dossier.getAgentId()).isEqualTo(agentId);
        assertThat(result.agentId()).isEqualTo(agentId);
        assertThat(result.agentNom()).isEqualTo("Diallo Fatou");
    }

    @Test
    void assignerAgent_whenAgentOtherOrganisation_shouldThrow() {
        agent.setOrganisationId(otherOrganisationId);
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> dossierService.assignerAgent(dossierId, agentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("AGENT_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(dossierRepository, never()).save(any());
    }

    @Test
    void assignerAgent_whenAgentInactive_shouldThrow() {
        agent.setActif(false);
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> dossierService.assignerAgent(dossierId, agentId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("AGENT_NOT_FOUND"));
    }

    @Test
    void update_shouldUpdateNotesPrioriteAndAgent() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(dossierRepository.save(any(DossierEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));
        when(commentaireRepository.findByDossierIdOrderByCreatedAtDesc(dossierId)).thenReturn(List.of());

        var result = dossierService.update(
                dossierId,
                new DossierUpdateDTO("notes maj", DossierPriorite.URGENTE, agentId),
                organisationId,
                userId
        );

        assertThat(dossier.getNotesInternes()).isEqualTo("notes maj");
        assertThat(dossier.getPriorite()).isEqualTo(DossierPriorite.URGENTE);
        assertThat(dossier.getAgentId()).isEqualTo(agentId);
        assertThat(dossier.getUpdatedBy()).isEqualTo(userId);
        assertThat(result.priorite()).isEqualTo(DossierPriorite.URGENTE);
    }

    @Test
    void transition_toRejeteWithoutMotif_shouldThrow() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));

        assertThatThrownBy(() -> dossierService.transition(
                dossierId,
                DossierStatut.REJETE,
                "  ",
                userId,
                "ROLE_SUPERVISEUR",
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("MOTIF_REQUIS");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(dossierRepository, never()).save(any());
    }

    @Test
    void transition_toIncomplet_shouldStoreMotifAndHistorique() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));
        when(dossierRepository.save(any(DossierEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(commentaireRepository.findByDossierIdOrderByCreatedAtDesc(dossierId)).thenReturn(List.of());

        var result = dossierService.transition(
                dossierId,
                DossierStatut.INCOMPLET,
                "Pièce manquante",
                userId,
                "ROLE_AGENT",
                organisationId
        );

        assertThat(dossier.getStatut()).isEqualTo(DossierStatut.INCOMPLET);
        assertThat(dossier.getMotifRejet()).isEqualTo("Pièce manquante");
        assertThat(result.statut()).isEqualTo(DossierStatut.INCOMPLET);

        ArgumentCaptor<DossierHistoriqueEntity> captor = ArgumentCaptor.forClass(DossierHistoriqueEntity.class);
        verify(historiqueRepository).save(captor.capture());
        assertThat(captor.getValue().getStatutDe()).isEqualTo("EN_REVUE");
        assertThat(captor.getValue().getStatutVers()).isEqualTo("INCOMPLET");
        assertThat(captor.getValue().getCommentaire()).isEqualTo("Pièce manquante");
    }

    @Test
    void getStats_shouldAggregateByStatutAndType() {
        when(dossierRepository.countByStatut(organisationId)).thenReturn(List.of(
                new Object[]{DossierStatut.BROUILLON, 3L},
                new Object[]{DossierStatut.SOUMIS, 5L}
        ));
        when(dossierRepository.countByTypeService(organisationId)).thenReturn(List.of(
                new Object[]{"Passeport", 4L},
                new Object[]{"Visa", 2L}
        ));

        var stats = dossierService.getStats(organisationId);

        assertThat(stats.parStatut().get(DossierStatut.BROUILLON)).isEqualTo(3L);
        assertThat(stats.parStatut().get(DossierStatut.SOUMIS)).isEqualTo(5L);
        assertThat(stats.parStatut().get(DossierStatut.CLOTURE)).isZero();
        assertThat(stats.parTypeService()).hasSize(2);
        assertThat(stats.parTypeService().getFirst().typeService()).isEqualTo("Passeport");
        assertThat(stats.parTypeService().getFirst().count()).isEqualTo(4L);
    }
}
