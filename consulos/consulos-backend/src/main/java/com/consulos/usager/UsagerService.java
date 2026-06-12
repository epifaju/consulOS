package com.consulos.usager;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.usager.dto.DossierSummaryDTO;
import com.consulos.usager.dto.DoublonGroupDTO;
import com.consulos.usager.dto.UsagerCreateDTO;
import com.consulos.usager.dto.UsagerCreateResponseDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.dto.UsagerSummaryDTO;
import com.consulos.usager.dto.UsagerUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsagerService {

    private static final UUID NO_EXCLUDE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String DOUBLON_MOTIF =
            "Usager(s) avec le même nom, date de naissance et nationalité";

    private final UsagerRepository usagerRepository;
    private final OrganisationRepository organisationRepository;
    private final UsagerMapper usagerMapper;

    @Transactional
    public UsagerCreateResponseDTO create(UsagerCreateDTO dto, UUID organisationId) {
        List<DoublonGroupDTO> doublons = Collections.emptyList();
        if (dto.dateNaissance() != null && StringUtils.hasText(dto.nationalite())) {
            doublons = findDoublons(dto.nom(), dto.dateNaissance(), dto.nationalite(), organisationId, NO_EXCLUDE_ID);
        }

        OrganisationEntity organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new BusinessException("ORG_NOT_FOUND", "Organisation introuvable", HttpStatus.NOT_FOUND));

        UsagerEntity entity = usagerMapper.toEntity(dto);
        entity.setOrganisationId(organisationId);
        entity.setNumero(generateNumero(organisation));
        if (entity.getTypeUsager() == null) {
            entity.setTypeUsager(TypeUsager.DIASPORA);
        }

        UsagerEntity saved = usagerRepository.save(entity);
        log.info("Usager créé: {} ({})", saved.getNumero(), saved.getId());

        return new UsagerCreateResponseDTO(usagerMapper.toDetail(saved), doublons);
    }

    @Transactional
    public UsagerDetailDTO update(UUID id, UsagerUpdateDTO dto, UUID organisationId) {
        UsagerEntity entity = getEntityForOrganisation(id, organisationId);
        usagerMapper.updateEntity(entity, dto);
        return usagerMapper.toDetail(usagerRepository.save(entity));
    }

    @Transactional
    public void softDelete(UUID id, UUID organisationId) {
        UsagerEntity entity = getEntityForOrganisation(id, organisationId);
        entity.setActif(false);
        usagerRepository.save(entity);
        log.info("Usager désactivé: {}", id);
    }

    @Transactional(readOnly = true)
    public UsagerDetailDTO findById(UUID id, UUID organisationId) {
        return usagerMapper.toDetail(getEntityForOrganisation(id, organisationId));
    }

    @Transactional(readOnly = true)
    public PageResponse<UsagerSummaryDTO> search(String query, UUID organisationId, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            Page<UsagerEntity> page = usagerRepository.findByOrganisationIdAndActifTrue(organisationId, pageable);
            return PageResponse.from(page.map(usagerMapper::toSummary));
        }
        List<UsagerSummaryDTO> results = usagerRepository.searchFullText(organisationId, query.trim())
                .stream()
                .map(usagerMapper::toSummary)
                .toList();
        return new PageResponse<>(results, results.size(), 1, 0, results.size(), true, true);
    }

    @Transactional(readOnly = true)
    public List<UsagerSummaryDTO> searchQuick(String query, UUID organisationId) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return usagerRepository.searchFullText(organisationId, query.trim())
                .stream()
                .map(usagerMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoublonGroupDTO> findDoublons(String nom,
                                               LocalDate dateNaissance,
                                               String nationalite,
                                               UUID organisationId,
                                               UUID excludeId) {
        if (!StringUtils.hasText(nom) || dateNaissance == null || !StringUtils.hasText(nationalite)) {
            return List.of();
        }
        UUID effectiveExcludeId = excludeId != null ? excludeId : NO_EXCLUDE_ID;
        List<UsagerSummaryDTO> similaires = usagerRepository
                .findPotentialDoublons(organisationId, nom, dateNaissance, nationalite, effectiveExcludeId)
                .stream()
                .map(usagerMapper::toSummary)
                .toList();
        if (similaires.isEmpty()) {
            return List.of();
        }
        return List.of(new DoublonGroupDTO(similaires, DOUBLON_MOTIF));
    }

    @Transactional(readOnly = true)
    public List<DossierSummaryDTO> getHistorique(UUID usagerId, UUID organisationId) {
        getEntityForOrganisation(usagerId, organisationId);
        return List.of();
    }

    private UsagerEntity getEntityForOrganisation(UUID id, UUID organisationId) {
        UsagerEntity entity = usagerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(entity.getOrganisationId()) || !Boolean.TRUE.equals(entity.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
        return entity;
    }

    private String generateNumero(OrganisationEntity organisation) {
        String prefix = organisation.getCode().contains("-")
                ? organisation.getCode().substring(0, organisation.getCode().indexOf('-'))
                : organisation.getCode();
        Long sequence = usagerRepository.nextUsagerSequence();
        return prefix + "-" + Year.now().getValue() + "-" + String.format("%05d", sequence);
    }
}
