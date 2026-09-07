package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.dto.DossierSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsagerDossierListQueryService {

    private final UsagerRepository usagerRepository;
    private final DossierRepository dossierRepository;
    private final TypeServiceRepository typeServiceRepository;

    @Transactional(readOnly = true)
    public List<DossierSummaryDTO> listDossiers(UUID usagerId, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(usagerId)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }

        List<DossierEntity> dossiers = dossierRepository
                .findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypes(dossiers);

        return dossiers.stream()
                .map(d -> {
                    TypeServiceEntity type = types.get(d.getTypeServiceId());
                    return new DossierSummaryDTO(
                            d.getId(),
                            d.getReference(),
                            type != null ? type.getLibelle() : null,
                            d.getStatut() != null ? d.getStatut().name() : null,
                            d.getCreatedAt()
                    );
                })
                .toList();
    }

    private Map<UUID, TypeServiceEntity> loadTypes(List<DossierEntity> dossiers) {
        List<UUID> ids = dossiers.stream().map(DossierEntity::getTypeServiceId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return typeServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TypeServiceEntity::getId, Function.identity()));
    }
}
