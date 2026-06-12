package com.consulos.dossier;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.dossier.dto.CommentaireCreateDTO;
import com.consulos.dossier.dto.CommentaireDTO;
import com.consulos.dossier.dto.DossierCreateDTO;
import com.consulos.dossier.dto.DossierDetailDTO;
import com.consulos.dossier.dto.DossierStatsDTO;
import com.consulos.dossier.dto.DossierSummaryDTO;
import com.consulos.dossier.dto.DossierUpdateDTO;
import com.consulos.dossier.dto.HistoriqueDTO;
import com.consulos.dossier.dto.TypeServiceCountDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository dossierRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final DossierHistoriqueRepository historiqueRepository;
    private final DossierCommentaireRepository commentaireRepository;
    private final UsagerRepository usagerRepository;
    private final UserRepository userRepository;
    private final WorkflowEngine workflowEngine;

    @Transactional
    @Auditable(action = "DOSSIER_CREATE")
    public DossierDetailDTO create(DossierCreateDTO dto, UUID organisationId, UUID createdByUserId) {
        UsagerEntity usager = getUsagerForOrganisation(dto.usagerId(), organisationId);
        TypeServiceEntity typeService = getTypeServiceForOrganisation(dto.typeServiceId(), organisationId);

        DossierEntity entity = new DossierEntity();
        entity.setOrganisationId(organisationId);
        entity.setReference(generateReference());
        entity.setUsagerId(usager.getId());
        entity.setTypeServiceId(typeService.getId());
        entity.setStatut(DossierStatut.BROUILLON);
        entity.setPriorite(dto.priorite() != null ? dto.priorite() : DossierPriorite.NORMALE);
        entity.setNotesInternes(dto.notesInternes());
        entity.setDateEcheance(OffsetDateTime.now().plusDays(
                typeService.getDelaiJours() != null ? typeService.getDelaiJours() : 30
        ));
        entity.setCreatedBy(createdByUserId);
        entity.setUpdatedBy(createdByUserId);
        entity.setActif(true);

        DossierEntity saved = dossierRepository.save(entity);
        createHistorique(saved.getId(), null, DossierStatut.BROUILLON, null, createdByUserId);

        log.info("Dossier créé: {} ({})", saved.getReference(), saved.getId());
        return toDetail(saved, usager, typeService);
    }

    @Transactional
    @Auditable(action = "DOSSIER_TRANSITION")
    public DossierDetailDTO transition(
            UUID dossierId,
            DossierStatut newStatut,
            String motif,
            UUID userId,
            String userRole,
            UUID organisationId
    ) {
        DossierEntity dossier = getDossierForOrganisation(dossierId, organisationId);
        TypeServiceEntity typeService = getTypeServiceForOrganisation(dossier.getTypeServiceId(), organisationId);

        workflowEngine.validateTransition(
                dossier.getStatut(),
                newStatut,
                typeService.getWorkflowConfig(),
                userRole,
                motif
        );

        if (newStatut == DossierStatut.SOUMIS && dossier.getDateSoumission() == null) {
            dossier.setDateSoumission(OffsetDateTime.now());
        }
        if (newStatut == DossierStatut.REJETE && !StringUtils.hasText(motif)) {
            throw new BusinessException("MOTIF_REQUIS", "Le motif de rejet est obligatoire", HttpStatus.BAD_REQUEST);
        }

        DossierStatut ancienStatut = dossier.getStatut();
        dossier.setStatut(newStatut);
        if (newStatut == DossierStatut.REJETE || newStatut == DossierStatut.INCOMPLET) {
            dossier.setMotifRejet(motif);
        }
        dossier.setUpdatedBy(userId);

        DossierEntity saved = dossierRepository.save(dossier);
        createHistorique(saved.getId(), ancienStatut, newStatut, motif, userId);

        UsagerEntity usager = getUsagerForOrganisation(saved.getUsagerId(), organisationId);
        return toDetail(saved, usager, typeService);
    }

    @Transactional
    @Auditable(action = "DOSSIER_ASSIGN_AGENT")
    public DossierDetailDTO assignerAgent(UUID dossierId, UUID agentId, UUID organisationId) {
        DossierEntity dossier = getDossierForOrganisation(dossierId, organisationId);
        UserEntity agent = userRepository.findById(agentId)
                .filter(u -> organisationId.equals(u.getOrganisationId()) && Boolean.TRUE.equals(u.getActif()))
                .orElseThrow(() -> new BusinessException("AGENT_NOT_FOUND", "Agent introuvable", HttpStatus.NOT_FOUND));

        dossier.setAgentId(agent.getId());
        DossierEntity saved = dossierRepository.save(dossier);

        UsagerEntity usager = getUsagerForOrganisation(saved.getUsagerId(), organisationId);
        TypeServiceEntity typeService = getTypeServiceForOrganisation(saved.getTypeServiceId(), organisationId);
        return toDetail(saved, usager, typeService);
    }

    @Transactional
    public CommentaireDTO ajouterCommentaire(
            UUID dossierId,
            CommentaireCreateDTO dto,
            UUID auteurId,
            UUID organisationId
    ) {
        getDossierForOrganisation(dossierId, organisationId);

        DossierCommentaireEntity commentaire = new DossierCommentaireEntity();
        commentaire.setDossierId(dossierId);
        commentaire.setAuteurId(auteurId);
        commentaire.setContenu(dto.contenu());

        DossierCommentaireEntity saved = commentaireRepository.save(commentaire);
        UserEntity auteur = userRepository.findById(auteurId).orElse(null);
        String auteurNom = auteur != null ? auteur.getNom() + " " + auteur.getPrenom() : null;

        return new CommentaireDTO(
                saved.getId(),
                saved.getAuteurId(),
                auteurNom,
                saved.getContenu(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<HistoriqueDTO> getHistorique(UUID dossierId, UUID organisationId) {
        getDossierForOrganisation(dossierId, organisationId);

        Map<UUID, UserEntity> usersById = new HashMap<>();
        return historiqueRepository.findByDossierIdOrderByCreatedAtAsc(dossierId).stream()
                .map(h -> toHistoriqueDto(h, usersById))
                .toList();
    }

    @Transactional(readOnly = true)
    public DossierStatsDTO getStats(UUID organisationId) {
        Map<DossierStatut, Long> parStatut = new EnumMap<>(DossierStatut.class);
        for (DossierStatut statut : DossierStatut.values()) {
            parStatut.put(statut, 0L);
        }
        for (Object[] row : dossierRepository.countByStatut(organisationId)) {
            parStatut.put((DossierStatut) row[0], (Long) row[1]);
        }

        List<TypeServiceCountDTO> parTypeService = dossierRepository.countByTypeService(organisationId).stream()
                .map(row -> new TypeServiceCountDTO((String) row[0], (Long) row[1]))
                .toList();

        return new DossierStatsDTO(parStatut, parTypeService);
    }

    @Transactional(readOnly = true)
    public PageResponse<DossierSummaryDTO> list(
            UUID organisationId,
            List<DossierStatut> statuts,
            UUID typeServiceId,
            UUID agentId,
            UUID usagerId,
            DossierPriorite priorite,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            Pageable pageable
    ) {
        Specification<DossierEntity> spec = Specification.where(DossierSpecifications.forOrganisation(organisationId))
                .and(DossierSpecifications.withStatuts(statuts))
                .and(DossierSpecifications.withTypeService(typeServiceId))
                .and(DossierSpecifications.withAgent(agentId))
                .and(DossierSpecifications.withUsager(usagerId))
                .and(DossierSpecifications.withPriorite(priorite))
                .and(DossierSpecifications.withCreatedBetween(dateDebut, dateFin));

        Page<DossierEntity> page = dossierRepository.findAll(spec, pageable);
        Map<UUID, UsagerEntity> usagers = loadUsagers(page.getContent(), organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypeServices(page.getContent(), organisationId);
        Map<UUID, UserEntity> agents = loadAgents(page.getContent());

        List<DossierSummaryDTO> content = page.getContent().stream()
                .map(d -> toSummary(d, usagers, types, agents))
                .toList();

        return new PageResponse<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public DossierDetailDTO findById(UUID dossierId, UUID organisationId) {
        DossierEntity dossier = getDossierForOrganisation(dossierId, organisationId);
        UsagerEntity usager = getUsagerForOrganisation(dossier.getUsagerId(), organisationId);
        TypeServiceEntity typeService = getTypeServiceForOrganisation(dossier.getTypeServiceId(), organisationId);
        return toDetail(dossier, usager, typeService);
    }

    @Transactional
    @Auditable(action = "DOSSIER_UPDATE")
    public DossierDetailDTO update(UUID dossierId, DossierUpdateDTO dto, UUID organisationId, UUID userId) {
        DossierEntity dossier = getDossierForOrganisation(dossierId, organisationId);

        if (dto.notesInternes() != null) {
            dossier.setNotesInternes(dto.notesInternes());
        }
        if (dto.priorite() != null) {
            dossier.setPriorite(dto.priorite());
        }
        if (dto.agentId() != null) {
            userRepository.findById(dto.agentId())
                    .filter(u -> organisationId.equals(u.getOrganisationId()) && Boolean.TRUE.equals(u.getActif()))
                    .orElseThrow(() -> new BusinessException("AGENT_NOT_FOUND", "Agent introuvable", HttpStatus.NOT_FOUND));
            dossier.setAgentId(dto.agentId());
        }
        dossier.setUpdatedBy(userId);

        DossierEntity saved = dossierRepository.save(dossier);
        UsagerEntity usager = getUsagerForOrganisation(saved.getUsagerId(), organisationId);
        TypeServiceEntity typeService = getTypeServiceForOrganisation(saved.getTypeServiceId(), organisationId);
        return toDetail(saved, usager, typeService);
    }

    private void createHistorique(
            UUID dossierId,
            DossierStatut statutDe,
            DossierStatut statutVers,
            String commentaire,
            UUID userId
    ) {
        DossierHistoriqueEntity historique = new DossierHistoriqueEntity();
        historique.setDossierId(dossierId);
        historique.setStatutDe(statutDe != null ? statutDe.name() : null);
        historique.setStatutVers(statutVers.name());
        historique.setCommentaire(commentaire);
        historique.setUserId(userId);
        historiqueRepository.save(historique);
    }

    private String generateReference() {
        Long sequence = dossierRepository.nextDossierSequence();
        return "DOS-" + Year.now().getValue() + "-" + String.format("%05d", sequence);
    }

    private DossierEntity getDossierForOrganisation(UUID id, UUID organisationId) {
        return dossierRepository.findByIdAndOrganisationIdAndActifTrue(id, organisationId)
                .orElseThrow(() -> new BusinessException("DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
    }

    private UsagerEntity getUsagerForOrganisation(UUID id, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
        return usager;
    }

    private TypeServiceEntity getTypeServiceForOrganisation(UUID id, UUID organisationId) {
        return typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(id, organisationId)
                .orElseThrow(() -> new BusinessException("TYPE_SERVICE_NOT_FOUND", "Type de service introuvable", HttpStatus.NOT_FOUND));
    }

    private DossierSummaryDTO toSummary(
            DossierEntity dossier,
            Map<UUID, UsagerEntity> usagers,
            Map<UUID, TypeServiceEntity> types,
            Map<UUID, UserEntity> agents
    ) {
        UsagerEntity usager = usagers.get(dossier.getUsagerId());
        TypeServiceEntity type = types.get(dossier.getTypeServiceId());
        UserEntity agent = dossier.getAgentId() != null ? agents.get(dossier.getAgentId()) : null;

        return new DossierSummaryDTO(
                dossier.getId(),
                dossier.getReference(),
                dossier.getUsagerId(),
                usager != null ? usager.getNom() : null,
                usager != null ? usager.getPrenom() : null,
                type != null ? type.getLibelle() : null,
                dossier.getStatut(),
                dossier.getAgentId(),
                agent != null ? agent.getNom() + " " + agent.getPrenom() : null,
                dossier.getPriorite(),
                dossier.getDateEcheance()
        );
    }

    private DossierDetailDTO toDetail(DossierEntity dossier, UsagerEntity usager, TypeServiceEntity typeService) {
        UserEntity agent = dossier.getAgentId() != null
                ? userRepository.findById(dossier.getAgentId()).orElse(null)
                : null;

        List<CommentaireDTO> commentaires = commentaireRepository
                .findByDossierIdOrderByCreatedAtDesc(dossier.getId()).stream()
                .map(c -> {
                    UserEntity auteur = c.getAuteurId() != null
                            ? userRepository.findById(c.getAuteurId()).orElse(null)
                            : null;
                    String auteurNom = auteur != null ? auteur.getNom() + " " + auteur.getPrenom() : null;
                    return new CommentaireDTO(c.getId(), c.getAuteurId(), auteurNom, c.getContenu(), c.getCreatedAt());
                })
                .toList();

        return new DossierDetailDTO(
                dossier.getId(),
                dossier.getReference(),
                dossier.getUsagerId(),
                usager.getNom(),
                usager.getPrenom(),
                typeService.getId(),
                typeService.getCode(),
                typeService.getLibelle(),
                dossier.getStatut(),
                dossier.getAgentId(),
                agent != null ? agent.getNom() + " " + agent.getPrenom() : null,
                dossier.getPriorite(),
                dossier.getDateSoumission(),
                dossier.getDateEcheance(),
                dossier.getMotifRejet(),
                dossier.getNotesInternes(),
                dossier.getMetadata(),
                typeService.getChecklist(),
                commentaires,
                dossier.getCreatedAt(),
                dossier.getUpdatedAt()
        );
    }

    private HistoriqueDTO toHistoriqueDto(DossierHistoriqueEntity h, Map<UUID, UserEntity> usersById) {
        UserEntity user = null;
        if (h.getUserId() != null) {
            user = usersById.computeIfAbsent(h.getUserId(), id -> userRepository.findById(id).orElse(null));
        }
        String userNom = user != null ? user.getNom() + " " + user.getPrenom() : null;

        DossierStatut statutDe = h.getStatutDe() != null ? DossierStatut.valueOf(h.getStatutDe()) : null;
        return new HistoriqueDTO(
                h.getId(),
                statutDe,
                DossierStatut.valueOf(h.getStatutVers()),
                h.getCommentaire(),
                h.getUserId(),
                userNom,
                h.getCreatedAt()
        );
    }

    private Map<UUID, UsagerEntity> loadUsagers(List<DossierEntity> dossiers, UUID organisationId) {
        List<UUID> ids = dossiers.stream().map(DossierEntity::getUsagerId).distinct().toList();
        return usagerRepository.findAllById(ids).stream()
                .filter(u -> organisationId.equals(u.getOrganisationId()))
                .collect(Collectors.toMap(UsagerEntity::getId, Function.identity()));
    }

    private Map<UUID, TypeServiceEntity> loadTypeServices(List<DossierEntity> dossiers, UUID organisationId) {
        List<UUID> ids = dossiers.stream().map(DossierEntity::getTypeServiceId).distinct().toList();
        return typeServiceRepository.findAllById(ids).stream()
                .filter(t -> organisationId.equals(t.getOrganisationId()))
                .collect(Collectors.toMap(TypeServiceEntity::getId, Function.identity()));
    }

    private Map<UUID, UserEntity> loadAgents(List<DossierEntity> dossiers) {
        List<UUID> ids = dossiers.stream()
                .map(DossierEntity::getAgentId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }
}
