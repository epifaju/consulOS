package com.consulos.encaissement;

import com.consulos.common.BusinessException;
import com.consulos.encaissement.dto.TarifDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TarifService {

    private final TarifRepository tarifRepository;

    @Transactional(readOnly = true)
    public List<TarifDTO> list(UUID organisationId) {
        return tarifRepository.findByOrganisationIdAndActifTrue(organisationId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TarifDTO create(TarifDTO dto, UUID organisationId) {
        TarifEntity entity = new TarifEntity();
        entity.setOrganisationId(organisationId);
        applyDto(entity, dto);
        return toDto(tarifRepository.save(entity));
    }

    @Transactional
    public TarifDTO update(UUID id, TarifDTO dto, UUID organisationId) {
        TarifEntity entity = tarifRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new BusinessException("TARIF_NOT_FOUND", "Tarif introuvable", HttpStatus.NOT_FOUND));
        applyDto(entity, dto);
        return toDto(tarifRepository.save(entity));
    }

    private void applyDto(TarifEntity entity, TarifDTO dto) {
        entity.setTypeServiceId(dto.typeServiceId());
        entity.setLibelle(dto.libelle());
        entity.setMontant(dto.montant());
        entity.setDevise(dto.devise() != null ? dto.devise() : "EUR");
        if (dto.actif() != null) {
            entity.setActif(dto.actif());
        }
    }

    private TarifDTO toDto(TarifEntity entity) {
        return new TarifDTO(
                entity.getId(),
                entity.getTypeServiceId(),
                entity.getLibelle(),
                entity.getMontant(),
                entity.getDevise(),
                entity.getActif()
        );
    }
}
