package com.consulos.administration;

import com.consulos.administration.dto.OrganisationDTO;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganisationService {

    private final OrganisationRepository organisationRepository;

    @Transactional(readOnly = true)
    public OrganisationDTO get(UUID organisationId) {
        return toDto(getEntity(organisationId));
    }

    @Transactional
    public OrganisationDTO update(UUID organisationId, OrganisationDTO dto) {
        OrganisationEntity entity = getEntity(organisationId);
        entity.setNom(dto.nom());
        entity.setPaysCode(dto.paysCode());
        if (dto.langueDefaut() != null) {
            entity.setLangueDefaut(dto.langueDefaut());
        }
        if (dto.deviseDefaut() != null) {
            entity.setDeviseDefaut(dto.deviseDefaut());
        }
        if (dto.actif() != null) {
            entity.setActif(dto.actif());
        }
        entity.setUpdatedAt(OffsetDateTime.now());
        return toDto(organisationRepository.save(entity));
    }

    private OrganisationEntity getEntity(UUID organisationId) {
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> new BusinessException("ORG_NOT_FOUND", "Organisation introuvable", HttpStatus.NOT_FOUND));
    }

    private OrganisationDTO toDto(OrganisationEntity entity) {
        return new OrganisationDTO(
                entity.getId(),
                entity.getCode(),
                entity.getNom(),
                entity.getPaysCode(),
                entity.getLangueDefaut(),
                entity.getDeviseDefaut(),
                entity.getActif()
        );
    }
}
