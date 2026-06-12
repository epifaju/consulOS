package com.consulos.rendezvous;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.rendezvous.dto.RdvAgendaItemDTO;
import com.consulos.rendezvous.dto.RdvCreateDTO;
import com.consulos.rendezvous.dto.RdvDetailDTO;
import com.consulos.rendezvous.dto.RdvSummaryDTO;
import com.consulos.rendezvous.dto.RdvUpdateDTO;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RdvService {

    private final RendezvousRepository rendezvousRepository;
    private final UsagerRepository usagerRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;
    private final DisponibiliteService disponibiliteService;
    private final RdvStatutEngine rdvStatutEngine;
    private final RdvNotificationService notificationService;

    @Transactional
    public RdvDetailDTO create(RdvCreateDTO dto, UUID organisationId) {
        UsagerEntity usager = getUsagerForOrganisation(dto.usagerId(), organisationId);

        if (dto.typeServiceId() != null) {
            typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(dto.typeServiceId(), organisationId)
                    .orElseThrow(() -> new BusinessException(
                            "TYPE_SERVICE_NOT_FOUND", "Type de service introuvable", HttpStatus.NOT_FOUND));
        }

        if (dto.dossierId() != null) {
            dossierRepository.findByIdAndOrganisationIdAndActifTrue(dto.dossierId(), organisationId)
                    .orElseThrow(() -> new BusinessException(
                            "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
        }

        int duree = dto.dureeMinutes() != null ? dto.dureeMinutes() : 30;

        if (!disponibiliteService.isCreneauDisponible(
                organisationId, dto.typeServiceId(), dto.dateHeure(), duree)) {
            throw new BusinessException(
                    "CRENEAU_COMPLET", "Ce créneau n'est plus disponible", HttpStatus.CONFLICT);
        }

        if (dto.agentId() != null) {
            validateAgent(dto.agentId(), organisationId);
            if (rendezvousRepository.existsByAgentIdAndDateHeureAndStatutNotIn(dto.agentId(), dto.dateHeure())) {
                throw new BusinessException(
                        "AGENT_INDISPONIBLE", "L'agent a déjà un rendez-vous à cette heure", HttpStatus.CONFLICT);
            }
        }

        RendezvousEntity entity = new RendezvousEntity();
        entity.setOrganisationId(organisationId);
        entity.setUsagerId(usager.getId());
        entity.setDossierId(dto.dossierId());
        entity.setTypeServiceId(dto.typeServiceId());
        entity.setAgentId(dto.agentId());
        entity.setDateHeure(dto.dateHeure());
        entity.setDureeMinutes(duree);
        entity.setNotes(dto.notes());
        entity.setStatut(RdvStatut.CONFIRME);

        RendezvousEntity saved = rendezvousRepository.save(entity);
        notificationService.sendConfirmation(saved);
        log.info("Rendez-vous créé: {} ({})", saved.getId(), saved.getDateHeure());

        return toDetail(saved, usager);
    }

    @Transactional
    @Auditable(action = "RDV_UPDATE_STATUT")
    public RdvDetailDTO updateStatut(
            UUID rdvId,
            RdvStatut newStatut,
            String motif,
            UUID organisationId
    ) {
        RendezvousEntity rdv = getRdvForOrganisation(rdvId, organisationId);
        rdvStatutEngine.validateTransition(rdv.getStatut(), newStatut);

        if (newStatut == RdvStatut.ANNULE || newStatut == RdvStatut.REPORTE) {
            rdv.setMotif(motif);
        }

        rdv.setStatut(newStatut);
        RendezvousEntity saved = rendezvousRepository.save(rdv);

        if (newStatut == RdvStatut.ANNULE) {
            notificationService.sendAnnulation(saved, motif);
        }

        UsagerEntity usager = getUsagerForOrganisation(saved.getUsagerId(), organisationId);
        return toDetail(saved, usager);
    }

    @Transactional
    public RdvDetailDTO update(UUID rdvId, RdvUpdateDTO dto, UUID organisationId) {
        RendezvousEntity rdv = getRdvForOrganisation(rdvId, organisationId);

        if (dto.agentId() != null) {
            validateAgent(dto.agentId(), organisationId);
            rdv.setAgentId(dto.agentId());
        }
        if (dto.notes() != null) {
            rdv.setNotes(dto.notes());
        }

        RendezvousEntity saved = rendezvousRepository.save(rdv);
        UsagerEntity usager = getUsagerForOrganisation(saved.getUsagerId(), organisationId);
        return toDetail(saved, usager);
    }

    @Transactional(readOnly = true)
    public RdvDetailDTO findById(UUID rdvId, UUID organisationId) {
        RendezvousEntity rdv = getRdvForOrganisation(rdvId, organisationId);
        UsagerEntity usager = getUsagerForOrganisation(rdv.getUsagerId(), organisationId);
        return toDetail(rdv, usager);
    }

    @Transactional(readOnly = true)
    public PageResponse<RdvSummaryDTO> list(
            UUID organisationId,
            RdvStatut statut,
            UUID agentId,
            UUID usagerId,
            UUID typeServiceId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            Pageable pageable
    ) {
        Specification<RendezvousEntity> spec = Specification.where(RdvSpecifications.forOrganisation(organisationId))
                .and(RdvSpecifications.withStatut(statut))
                .and(RdvSpecifications.withAgent(agentId))
                .and(RdvSpecifications.withUsager(usagerId))
                .and(RdvSpecifications.withTypeService(typeServiceId))
                .and(RdvSpecifications.betweenDates(dateDebut, dateFin));

        Page<RendezvousEntity> page = rendezvousRepository.findAll(spec, pageable);
        Map<UUID, UsagerEntity> usagers = loadUsagers(page.getContent(), organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypes(page.getContent(), organisationId);
        Map<UUID, UserEntity> agents = loadAgents(page.getContent());

        List<RdvSummaryDTO> content = page.getContent().stream()
                .map(r -> toSummary(r, usagers, types, agents))
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
    public List<RdvAgendaItemDTO> getAgenda(
            UUID organisationId,
            LocalDate dateDebut,
            LocalDate dateFin,
            UUID agentId
    ) {
        OffsetDateTime debut = dateDebut.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = dateFin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<RendezvousEntity> rdvs = rendezvousRepository.findAgenda(organisationId, debut, fin, agentId);
        Map<UUID, UsagerEntity> usagers = loadUsagers(rdvs, organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypes(rdvs, organisationId);
        Map<UUID, UserEntity> agents = loadAgents(rdvs);

        return rdvs.stream()
                .map(r -> toAgendaItem(r, usagers, types, agents))
                .toList();
    }

    private void validateAgent(UUID agentId, UUID organisationId) {
        userRepository.findById(agentId)
                .filter(u -> organisationId.equals(u.getOrganisationId()) && Boolean.TRUE.equals(u.getActif()))
                .orElseThrow(() -> new BusinessException("AGENT_NOT_FOUND", "Agent introuvable", HttpStatus.NOT_FOUND));
    }

    private RendezvousEntity getRdvForOrganisation(UUID id, UUID organisationId) {
        return rendezvousRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new BusinessException("RDV_NOT_FOUND", "Rendez-vous introuvable", HttpStatus.NOT_FOUND));
    }

    private UsagerEntity getUsagerForOrganisation(UUID id, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
        return usager;
    }

    private RdvSummaryDTO toSummary(
            RendezvousEntity rdv,
            Map<UUID, UsagerEntity> usagers,
            Map<UUID, TypeServiceEntity> types,
            Map<UUID, UserEntity> agents
    ) {
        UsagerEntity usager = usagers.get(rdv.getUsagerId());
        TypeServiceEntity type = rdv.getTypeServiceId() != null ? types.get(rdv.getTypeServiceId()) : null;
        UserEntity agent = rdv.getAgentId() != null ? agents.get(rdv.getAgentId()) : null;

        return new RdvSummaryDTO(
                rdv.getId(),
                rdv.getUsagerId(),
                usager != null ? usager.getNom() : null,
                usager != null ? usager.getPrenom() : null,
                rdv.getTypeServiceId(),
                type != null ? type.getLibelle() : null,
                rdv.getAgentId(),
                agent != null ? agent.getNom() + " " + agent.getPrenom() : null,
                rdv.getDateHeure(),
                rdv.getDureeMinutes(),
                rdv.getStatut()
        );
    }

    private RdvAgendaItemDTO toAgendaItem(
            RendezvousEntity rdv,
            Map<UUID, UsagerEntity> usagers,
            Map<UUID, TypeServiceEntity> types,
            Map<UUID, UserEntity> agents
    ) {
        RdvSummaryDTO summary = toSummary(rdv, usagers, types, agents);
        return new RdvAgendaItemDTO(
                summary.id(),
                summary.usagerId(),
                summary.usagerNom(),
                summary.usagerPrenom(),
                summary.typeServiceId(),
                summary.typeServiceLibelle(),
                summary.agentId(),
                summary.agentNom(),
                summary.dateHeure(),
                summary.dureeMinutes(),
                summary.statut()
        );
    }

    private RdvDetailDTO toDetail(RendezvousEntity rdv, UsagerEntity usager) {
        TypeServiceEntity type = rdv.getTypeServiceId() != null
                ? typeServiceRepository.findById(rdv.getTypeServiceId()).orElse(null)
                : null;
        UserEntity agent = rdv.getAgentId() != null
                ? userRepository.findById(rdv.getAgentId()).orElse(null)
                : null;
        DossierEntity dossier = rdv.getDossierId() != null
                ? dossierRepository.findById(rdv.getDossierId()).orElse(null)
                : null;

        return new RdvDetailDTO(
                rdv.getId(),
                rdv.getUsagerId(),
                usager.getNom(),
                usager.getPrenom(),
                usager.getEmail(),
                rdv.getDossierId(),
                dossier != null ? dossier.getReference() : null,
                rdv.getTypeServiceId(),
                type != null ? type.getLibelle() : null,
                rdv.getAgentId(),
                agent != null ? agent.getNom() + " " + agent.getPrenom() : null,
                rdv.getDateHeure(),
                rdv.getDureeMinutes(),
                rdv.getStatut(),
                rdv.getMotif(),
                rdv.getNotes(),
                rdv.getRappelEnvoye(),
                rdv.getCreatedAt(),
                rdv.getUpdatedAt()
        );
    }

    private Map<UUID, UsagerEntity> loadUsagers(List<RendezvousEntity> rdvs, UUID organisationId) {
        List<UUID> ids = rdvs.stream().map(RendezvousEntity::getUsagerId).distinct().toList();
        return usagerRepository.findAllById(ids).stream()
                .filter(u -> organisationId.equals(u.getOrganisationId()))
                .collect(Collectors.toMap(UsagerEntity::getId, Function.identity()));
    }

    private Map<UUID, TypeServiceEntity> loadTypes(List<RendezvousEntity> rdvs, UUID organisationId) {
        List<UUID> ids = rdvs.stream()
                .map(RendezvousEntity::getTypeServiceId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return typeServiceRepository.findAllById(ids).stream()
                .filter(t -> organisationId.equals(t.getOrganisationId()))
                .collect(Collectors.toMap(TypeServiceEntity::getId, Function.identity()));
    }

    private Map<UUID, UserEntity> loadAgents(List<RendezvousEntity> rdvs) {
        List<UUID> ids = rdvs.stream()
                .map(RendezvousEntity::getAgentId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }
}
