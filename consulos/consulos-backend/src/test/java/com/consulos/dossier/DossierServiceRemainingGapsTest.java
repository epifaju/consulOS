package com.consulos.dossier;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.dossier.dto.CommentaireCreateDTO;
import com.consulos.dossier.dto.CommentaireDTO;
import com.consulos.dossier.dto.DossierCreateDTO;
import com.consulos.dossier.dto.DossierSummaryDTO;
import com.consulos.dossier.dto.DossierUpdateDTO;
import com.consulos.dossier.dto.HistoriqueDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Gaps remaining after DossierServiceTest / QueryTest / EdgeCasesTest.
 */
@ExtendWith(MockitoExtension.class)
class DossierServiceRemainingGapsTest {

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
    private UUID dossierId;
    private UUID usagerId;
    private UUID typeServiceId;
    private UUID agentId;
    private UUID auteurId;
    private UUID userId;
    private DossierEntity dossier;
    private UsagerEntity usager;
    private TypeServiceEntity typeService;
    private UserEntity agent;

    @BeforeEach
    void setUp() throws Exception {
        organisationId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        auteurId = UUID.randomUUID();
        userId = UUID.randomUUID();

        dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setReference("DOS-2026-00100");
        dossier.setStatut(DossierStatut.EN_REVUE);
        dossier.setPriorite(DossierPriorite.NORMALE);
        dossier.setActif(true);
        dossier.setAgentId(agentId);
        dossier.setDateEcheance(OffsetDateTime.parse("2026-09-20T00:00:00Z"));

        usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");

        typeService = new TypeServiceEntity();
        typeService.setId(typeServiceId);
        typeService.setOrganisationId(organisationId);
        typeService.setActif(true);
        typeService.setCode("PASSEPORT");
        typeService.setLibelle("Passeport");
        typeService.setChecklist(objectMapper.readTree("[]"));

        agent = new UserEntity();
        agent.setId(agentId);
        agent.setOrganisationId(organisationId);
        agent.setActif(true);
        agent.setNom("Diallo");
        agent.setPrenom("Fatou");
    }

    @Test
    void findById_whenMissingOrWrongOrg_shouldThrow() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.findById(dossierId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(usagerRepository, never()).findById(any());
    }

    @Test
    void list_whenEmpty_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(dossierRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<DossierSummaryDTO> page = dossierService.list(
                organisationId, null, null, null, null, null, null, null, pageable);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
    }

    @Test
    void list_whenAgentPresent_shouldEnrichAgentNom() {
        Pageable pageable = PageRequest.of(0, 20);
        when(dossierRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dossier), pageable, 1));
        when(usagerRepository.findAllById(anyCollection())).thenReturn(List.of(usager));
        when(typeServiceRepository.findAllById(anyCollection())).thenReturn(List.of(typeService));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(agent));

        PageResponse<DossierSummaryDTO> page = dossierService.list(
                organisationId, null, null, null, null, null, null, null, pageable);

        DossierSummaryDTO summary = page.content().getFirst();
        assertThat(summary.agentId()).isEqualTo(agentId);
        assertThat(summary.agentNom()).isEqualTo("Diallo Fatou");
    }

    @Test
    void ajouterCommentaire_whenAuteurMissing_shouldNullAuteurNom() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(commentaireRepository.save(any(DossierCommentaireEntity.class))).thenAnswer(inv -> {
            DossierCommentaireEntity c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(userRepository.findById(auteurId)).thenReturn(Optional.empty());

        CommentaireDTO result = dossierService.ajouterCommentaire(
                dossierId, new CommentaireCreateDTO("Note"), auteurId, organisationId);

        assertThat(result.auteurId()).isEqualTo(auteurId);
        assertThat(result.auteurNom()).isNull();
        assertThat(result.contenu()).isEqualTo("Note");
    }

    @Test
    void getHistorique_whenEmpty_shouldReturnEmpty() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(historiqueRepository.findByDossierIdOrderByCreatedAtAsc(dossierId)).thenReturn(List.of());

        List<HistoriqueDTO> result = dossierService.getHistorique(dossierId, organisationId);

        assertThat(result).isEmpty();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getHistorique_whenStatutDeNull_shouldMap() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));

        UUID histId = UUID.randomUUID();
        DossierHistoriqueEntity hist = new DossierHistoriqueEntity();
        hist.setId(histId);
        hist.setDossierId(dossierId);
        hist.setStatutDe(null);
        hist.setStatutVers("BROUILLON");
        hist.setCommentaire(null);
        hist.setUserId(null);
        hist.setCreatedAt(OffsetDateTime.parse("2026-09-09T10:00:00Z"));

        when(historiqueRepository.findByDossierIdOrderByCreatedAtAsc(dossierId)).thenReturn(List.of(hist));

        List<HistoriqueDTO> result = dossierService.getHistorique(dossierId, organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().statutDe()).isNull();
        assertThat(result.getFirst().statutVers()).isEqualTo(DossierStatut.BROUILLON);
        assertThat(result.getFirst().userNom()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void create_whenUsagerWrongOrgOrInactive_shouldThrow() {
        usager.setOrganisationId(UUID.randomUUID());
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> dossierService.create(
                new DossierCreateDTO(usagerId, typeServiceId, null, null),
                organisationId,
                userId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(dossierRepository, never()).save(any());
    }

    @Test
    void update_whenAgentNotFound_shouldThrow() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(userRepository.findById(agentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.update(
                dossierId,
                new DossierUpdateDTO(null, null, agentId),
                organisationId,
                userId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("AGENT_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(dossierRepository, never()).save(any());
    }
}
