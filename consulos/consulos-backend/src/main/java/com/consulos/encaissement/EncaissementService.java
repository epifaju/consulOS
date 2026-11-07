package com.consulos.encaissement;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.dto.EncaissementCreateDTO;
import com.consulos.encaissement.dto.EncaissementDetailDTO;
import com.consulos.encaissement.dto.EncaissementSummaryDTO;
import com.consulos.encaissement.dto.JournalCaisseDTO;
import com.consulos.encaissement.dto.ModeTotalDTO;
import com.consulos.encaissement.dto.PaiementDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncaissementService {

    private final EncaissementRepository encaissementRepository;
    private final UsagerRepository usagerRepository;
    private final DossierRepository dossierRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final RecuPdfService recuPdfService;

    @Transactional
    @Auditable(action = "CREATE_ENCAISSEMENT")
    public EncaissementDetailDTO create(EncaissementCreateDTO dto, UUID caissierUserId, UUID organisationId) {
        UsagerEntity usager = getUsagerForOrganisation(dto.usagerId(), organisationId);
        DossierEntity dossier = null;
        if (dto.dossierId() != null) {
            dossier = dossierRepository.findByIdAndOrganisationIdAndActifTrue(dto.dossierId(), organisationId)
                    .orElseThrow(() -> new BusinessException(
                            "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
            if (!usager.getId().equals(dossier.getUsagerId())) {
                throw new BusinessException(
                        "DOSSIER_USAGER_MISMATCH", "Le dossier n'appartient pas à cet usager", HttpStatus.BAD_REQUEST);
            }
        }

        EncaissementEntity entity = new EncaissementEntity();
        entity.setOrganisationId(organisationId);
        entity.setUsagerId(usager.getId());
        entity.setDossierId(dto.dossierId());
        entity.setNumeroRecu(generateNumeroRecu());
        entity.setMontantTotal(dto.montantTotal());
        entity.setMontantPercu(BigDecimal.ZERO);
        entity.setDevise(StringUtils.hasText(dto.devise()) ? dto.devise() : "EUR");
        entity.setStatut(StatutEncaissement.DU);
        entity.setCaissierId(caissierUserId);
        entity.setNotes(dto.notes());
        if (dto.modePaiement() == ModePaiement.EXONERE) {
            entity.setStatut(StatutEncaissement.EXONERE);
            entity.setModePaiement(ModePaiement.EXONERE);
        } else if (dto.modePaiement() != null) {
            entity.setModePaiement(dto.modePaiement());
            entity.setMontantPercu(dto.montantTotal());
            entity.setStatut(StatutEncaissement.PAYE);
            entity.setDatePaiement(OffsetDateTime.now());
        }

        EncaissementEntity saved = encaissementRepository.save(entity);
        log.info("Encaissement créé: {} ({})", saved.getNumeroRecu(), saved.getId());
        return toDetail(saved, usager, dossier);
    }

    @Transactional
    @Auditable(action = "PAIEMENT_ENCAISSEMENT")
    public EncaissementDetailDTO enregistrerPaiement(UUID encId, PaiementDTO dto, UUID organisationId) {
        EncaissementEntity enc = getEncaissementForOrganisation(encId, organisationId);

        if (enc.getStatut() == StatutEncaissement.EXONERE || enc.getStatut() == StatutEncaissement.REMBOURSE) {
            throw new BusinessException(
                    "PAIEMENT_NON_AUTORISE", "Paiement non autorisé pour ce statut", HttpStatus.BAD_REQUEST);
        }

        BigDecimal percu = enc.getMontantPercu() != null ? enc.getMontantPercu() : BigDecimal.ZERO;
        BigDecimal nouveauPercu = percu.add(dto.montant());
        if (nouveauPercu.compareTo(enc.getMontantTotal()) > 0) {
            throw new BusinessException(
                    "MONTANT_EXCESSIF", "Le montant dépasse le reste dû", HttpStatus.BAD_REQUEST);
        }

        enc.setMontantPercu(nouveauPercu);
        enc.setModePaiement(dto.modePaiement());
        enc.setDatePaiement(OffsetDateTime.now());
        if (dto.notes() != null) {
            enc.setNotes(dto.notes());
        }

        if (nouveauPercu.compareTo(enc.getMontantTotal()) >= 0) {
            enc.setStatut(StatutEncaissement.PAYE);
        } else {
            enc.setStatut(StatutEncaissement.PARTIEL);
        }

        EncaissementEntity saved = encaissementRepository.save(enc);
        UsagerEntity usager = getUsagerForOrganisation(saved.getUsagerId(), organisationId);
        DossierEntity dossier = saved.getDossierId() != null
                ? dossierRepository.findById(saved.getDossierId()).orElse(null)
                : null;
        return toDetail(saved, usager, dossier);
    }

    @Transactional(readOnly = true)
    public EncaissementDetailDTO findById(UUID encId, UUID organisationId) {
        EncaissementEntity enc = getEncaissementForOrganisation(encId, organisationId);
        UsagerEntity usager = getUsagerForOrganisation(enc.getUsagerId(), organisationId);
        DossierEntity dossier = enc.getDossierId() != null
                ? dossierRepository.findById(enc.getDossierId()).orElse(null)
                : null;
        return toDetail(enc, usager, dossier);
    }

    @Transactional(readOnly = true)
    public PageResponse<EncaissementSummaryDTO> list(
            UUID organisationId,
            StatutEncaissement statut,
            ModePaiement modePaiement,
            UUID caissierId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            Pageable pageable
    ) {
        Specification<EncaissementEntity> spec = Specification.where(EncaissementSpecifications.forOrganisation(organisationId))
                .and(EncaissementSpecifications.withStatut(statut))
                .and(EncaissementSpecifications.withModePaiement(modePaiement))
                .and(EncaissementSpecifications.withCaissier(caissierId))
                .and(EncaissementSpecifications.betweenDates(dateDebut, dateFin));

        Page<EncaissementEntity> page = encaissementRepository.findAll(spec, pageable);
        Map<UUID, UsagerEntity> usagers = loadUsagers(page.getContent(), organisationId);
        Map<UUID, DossierEntity> dossiers = loadDossiers(page.getContent(), organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypes(dossiers.values());

        List<EncaissementSummaryDTO> content = page.getContent().stream()
                .map(e -> toSummary(e, usagers, dossiers, types))
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
    public JournalCaisseDTO getJournalCaisse(LocalDate date, UUID organisationId) {
        OffsetDateTime debut = EncaissementDayBounds.startOfDay(date);
        OffsetDateTime fin = EncaissementDayBounds.startOfNextDay(date);

        List<ModeTotalDTO> parMode = new ArrayList<>();
        long totalTransactions = 0;
        BigDecimal montantTotal = BigDecimal.ZERO;

        for (Object[] row : encaissementRepository.sumByModeForDay(organisationId, debut, fin)) {
            ModePaiement mode = (ModePaiement) row[0];
            long count = (Long) row[1];
            BigDecimal sum = (BigDecimal) row[2];
            parMode.add(new ModeTotalDTO(mode.name(), count, sum));
            totalTransactions += count;
            montantTotal = montantTotal.add(sum);
        }

        return new JournalCaisseDTO(date, parMode, totalTransactions, montantTotal);
    }

    @Transactional(readOnly = true)
    public byte[] getRecu(UUID encId, UUID organisationId) {
        EncaissementEntity enc = getEncaissementForOrganisation(encId, organisationId);
        UsagerEntity usager = getUsagerForOrganisation(enc.getUsagerId(), organisationId);
        DossierEntity dossier = enc.getDossierId() != null
                ? dossierRepository.findById(enc.getDossierId()).orElse(null)
                : null;
        TypeServiceEntity typeService = null;
        if (dossier != null) {
            typeService = typeServiceRepository.findById(dossier.getTypeServiceId()).orElse(null);
        }
        var org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new BusinessException("ORG_NOT_FOUND", "Organisation introuvable", HttpStatus.NOT_FOUND));
        UserEntity caissier = enc.getCaissierId() != null
                ? userRepository.findById(enc.getCaissierId()).orElse(null)
                : null;

        return recuPdfService.generateRecu(enc, usager, dossier, typeService, org, caissier);
    }

    private String generateNumeroRecu() {
        Long seq = encaissementRepository.nextEncaissementSequence();
        return "REC-" + Year.now().getValue() + "-" + String.format("%05d", seq);
    }

    private EncaissementEntity getEncaissementForOrganisation(UUID id, UUID organisationId) {
        return encaissementRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "ENCAISSEMENT_NOT_FOUND", "Encaissement introuvable", HttpStatus.NOT_FOUND));
    }

    private UsagerEntity getUsagerForOrganisation(UUID id, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
        return usager;
    }

    private EncaissementSummaryDTO toSummary(
            EncaissementEntity enc,
            Map<UUID, UsagerEntity> usagers,
            Map<UUID, DossierEntity> dossiers,
            Map<UUID, TypeServiceEntity> types
    ) {
        UsagerEntity usager = usagers.get(enc.getUsagerId());
        DossierEntity dossier = enc.getDossierId() != null ? dossiers.get(enc.getDossierId()) : null;
        TypeServiceEntity type = dossier != null ? types.get(dossier.getTypeServiceId()) : null;

        return new EncaissementSummaryDTO(
                enc.getId(),
                enc.getNumeroRecu(),
                enc.getUsagerId(),
                usager != null ? usager.getNom() : null,
                usager != null ? usager.getPrenom() : null,
                enc.getDossierId(),
                dossier != null ? dossier.getReference() : null,
                type != null ? type.getLibelle() : null,
                enc.getMontantTotal(),
                enc.getMontantPercu(),
                enc.getDevise(),
                enc.getModePaiement(),
                enc.getStatut(),
                enc.getCreatedAt()
        );
    }

    private EncaissementDetailDTO toDetail(EncaissementEntity enc, UsagerEntity usager, DossierEntity dossier) {
        TypeServiceEntity type = null;
        if (dossier != null) {
            type = typeServiceRepository.findById(dossier.getTypeServiceId()).orElse(null);
        }
        UserEntity caissier = enc.getCaissierId() != null
                ? userRepository.findById(enc.getCaissierId()).orElse(null)
                : null;

        return new EncaissementDetailDTO(
                enc.getId(),
                enc.getNumeroRecu(),
                enc.getUsagerId(),
                usager.getNom(),
                usager.getPrenom(),
                usager.getNumero(),
                enc.getDossierId(),
                dossier != null ? dossier.getReference() : null,
                type != null ? type.getLibelle() : null,
                enc.getMontantTotal(),
                enc.getMontantPercu(),
                enc.getDevise(),
                enc.getModePaiement(),
                enc.getStatut(),
                enc.getDatePaiement(),
                enc.getCaissierId(),
                caissier != null ? caissier.getPrenom() + " " + caissier.getNom() : null,
                enc.getNotes(),
                enc.getCreatedAt(),
                enc.getUpdatedAt()
        );
    }

    private Map<UUID, UsagerEntity> loadUsagers(List<EncaissementEntity> encs, UUID organisationId) {
        List<UUID> ids = encs.stream().map(EncaissementEntity::getUsagerId).distinct().toList();
        return usagerRepository.findAllById(ids).stream()
                .filter(u -> organisationId.equals(u.getOrganisationId()))
                .collect(Collectors.toMap(UsagerEntity::getId, Function.identity()));
    }

    private Map<UUID, DossierEntity> loadDossiers(List<EncaissementEntity> encs, UUID organisationId) {
        List<UUID> ids = encs.stream()
                .map(EncaissementEntity::getDossierId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return dossierRepository.findAllById(ids).stream()
                .filter(d -> organisationId.equals(d.getOrganisationId()))
                .collect(Collectors.toMap(DossierEntity::getId, Function.identity()));
    }

    private Map<UUID, TypeServiceEntity> loadTypes(java.util.Collection<DossierEntity> dossiers) {
        List<UUID> ids = dossiers.stream().map(DossierEntity::getTypeServiceId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return typeServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TypeServiceEntity::getId, Function.identity()));
    }
}
