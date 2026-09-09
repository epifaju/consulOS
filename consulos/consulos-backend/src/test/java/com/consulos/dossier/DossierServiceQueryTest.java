package com.consulos.dossier;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.dossier.dto.CommentaireCreateDTO;
import com.consulos.dossier.dto.CommentaireDTO;
import com.consulos.dossier.dto.DossierDetailDTO;
import com.consulos.dossier.dto.DossierSummaryDTO;
import com.consulos.dossier.dto.HistoriqueDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Densification : commentaires, historique, list, findById.
 */
@ExtendWith(MockitoExtension.class)
class DossierServiceQueryTest {

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

    private UUID organisationId;
    private UUID dossierId;
    private UUID usagerId;
    private UUID typeServiceId;
    private UUID auteurId;
    private DossierEntity dossier;
    private UsagerEntity usager;
    private TypeServiceEntity typeService;
    private UserEntity auteur;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        auteurId = UUID.randomUUID();

        dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setReference("DOS-2026-00088");
        dossier.setStatut(DossierStatut.EN_REVUE);
        dossier.setPriorite(DossierPriorite.NORMALE);
        dossier.setActif(true);
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

        auteur = new UserEntity();
        auteur.setId(auteurId);
        auteur.setOrganisationId(organisationId);
        auteur.setActif(true);
        auteur.setNom("Diallo");
        auteur.setPrenom("Fatou");
    }

    @Test
    void ajouterCommentaire_shouldPersistAndReturnAuteurNom() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(commentaireRepository.save(any(DossierCommentaireEntity.class))).thenAnswer(inv -> {
            DossierCommentaireEntity c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(userRepository.findById(auteurId)).thenReturn(Optional.of(auteur));

        CommentaireDTO result = dossierService.ajouterCommentaire(
                dossierId, new CommentaireCreateDTO("Pièce manquante"), auteurId, organisationId);

        assertThat(result.contenu()).isEqualTo("Pièce manquante");
        assertThat(result.auteurId()).isEqualTo(auteurId);
        assertThat(result.auteurNom()).isEqualTo("Diallo Fatou");
        assertThat(result.id()).isNotNull();

        ArgumentCaptor<DossierCommentaireEntity> captor = ArgumentCaptor.forClass(DossierCommentaireEntity.class);
        verify(commentaireRepository).save(captor.capture());
        assertThat(captor.getValue().getDossierId()).isEqualTo(dossierId);
        assertThat(captor.getValue().getAuteurId()).isEqualTo(auteurId);
    }

    @Test
    void ajouterCommentaire_whenDossierMissing_shouldThrow() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.ajouterCommentaire(
                dossierId, new CommentaireCreateDTO("x"), auteurId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(commentaireRepository, never()).save(any());
    }

    @Test
    void getHistorique_shouldMapStatutsAndUserNom() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));

        UUID histId = UUID.randomUUID();
        DossierHistoriqueEntity hist = new DossierHistoriqueEntity();
        hist.setId(histId);
        hist.setDossierId(dossierId);
        hist.setStatutDe("SOUMIS");
        hist.setStatutVers("EN_REVUE");
        hist.setCommentaire("Prise en charge");
        hist.setUserId(auteurId);
        hist.setCreatedAt(OffsetDateTime.parse("2026-09-09T11:00:00Z"));

        when(historiqueRepository.findByDossierIdOrderByCreatedAtAsc(dossierId)).thenReturn(List.of(hist));
        when(userRepository.findById(auteurId)).thenReturn(Optional.of(auteur));

        List<HistoriqueDTO> result = dossierService.getHistorique(dossierId, organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(histId);
        assertThat(result.getFirst().statutDe()).isEqualTo(DossierStatut.SOUMIS);
        assertThat(result.getFirst().statutVers()).isEqualTo(DossierStatut.EN_REVUE);
        assertThat(result.getFirst().commentaire()).isEqualTo("Prise en charge");
        assertThat(result.getFirst().userNom()).isEqualTo("Diallo Fatou");
    }

    @Test
    void getHistorique_whenDossierWrongOrg_shouldThrow() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.getHistorique(dossierId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND"));

        verify(historiqueRepository, never()).findByDossierIdOrderByCreatedAtAsc(any());
    }

    @Test
    void list_shouldMapSummaryWithUsagerAndType() {
        Pageable pageable = PageRequest.of(0, 20);
        when(dossierRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dossier), pageable, 1));
        when(usagerRepository.findAllById(anyCollection())).thenReturn(List.of(usager));
        when(typeServiceRepository.findAllById(anyCollection())).thenReturn(List.of(typeService));

        PageResponse<DossierSummaryDTO> page = dossierService.list(
                organisationId, null, null, null, null, null, null, null, pageable);

        assertThat(page.content()).hasSize(1);
        DossierSummaryDTO summary = page.content().getFirst();
        assertThat(summary.reference()).isEqualTo("DOS-2026-00088");
        assertThat(summary.usagerNom()).isEqualTo("Camara");
        assertThat(summary.usagerPrenom()).isEqualTo("Aminata");
        assertThat(summary.typeServiceLibelle()).isEqualTo("Passeport");
        assertThat(summary.statut()).isEqualTo(DossierStatut.EN_REVUE);
        assertThat(summary.priorite()).isEqualTo(DossierPriorite.NORMALE);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findById_shouldIncludeCommentaires() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));

        UUID commentId = UUID.randomUUID();
        DossierCommentaireEntity commentaire = new DossierCommentaireEntity();
        commentaire.setId(commentId);
        commentaire.setDossierId(dossierId);
        commentaire.setAuteurId(auteurId);
        commentaire.setContenu("Relance usager");
        commentaire.setCreatedAt(OffsetDateTime.parse("2026-09-09T12:00:00Z"));

        when(commentaireRepository.findByDossierIdOrderByCreatedAtDesc(dossierId))
                .thenReturn(List.of(commentaire));
        when(userRepository.findById(auteurId)).thenReturn(Optional.of(auteur));

        DossierDetailDTO detail = dossierService.findById(dossierId, organisationId);

        assertThat(detail.reference()).isEqualTo("DOS-2026-00088");
        assertThat(detail.commentaires()).hasSize(1);
        assertThat(detail.commentaires().getFirst().contenu()).isEqualTo("Relance usager");
        assertThat(detail.commentaires().getFirst().auteurNom()).isEqualTo("Diallo Fatou");
    }
}
