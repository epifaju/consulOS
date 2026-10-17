package com.consulos.dossier.checklist;

import com.consulos.common.BusinessException;
import com.consulos.dossier.Auditable;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.dossier.checklist.dto.DossierChecklistItemDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DossierChecklistService {

    private final DossierRepository dossierRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final DossierChecklistStatutRepository statutRepository;

    @Transactional(readOnly = true)
    public List<DossierChecklistItemDTO> list(UUID dossierId, UUID organisationId) {
        DossierEntity dossier = requireDossier(dossierId, organisationId);
        TypeServiceEntity typeService = requireTypeService(dossier.getTypeServiceId(), organisationId);
        Map<String, Boolean> statuts = statutRepository
                .findByDossierIdAndOrganisationId(dossierId, organisationId)
                .stream()
                .collect(Collectors.toMap(
                        DossierChecklistStatutEntity::getCode,
                        item -> Boolean.TRUE.equals(item.getValide()),
                        (a, b) -> a));
        List<DossierChecklistItemDTO> items = new ArrayList<>();
        for (JsonNode node : templateItems(typeService.getChecklist())) {
            String code = node.path("code").asText("");
            if (!StringUtils.hasText(code)) {
                continue;
            }
            String libelle = node.path("libelle").asText(code);
            items.add(new DossierChecklistItemDTO(code, libelle, statuts.getOrDefault(code, false)));
        }
        return items;
    }

    @Transactional
    @Auditable(action = "DOSSIER_CHECKLIST_UPDATE")
    public List<DossierChecklistItemDTO> update(
            UUID dossierId, String code, boolean valide, UUID organisationId
    ) {
        DossierEntity dossier = requireDossier(dossierId, organisationId);
        TypeServiceEntity typeService = requireTypeService(dossier.getTypeServiceId(), organisationId);
        if (!templateContains(typeService.getChecklist(), code)) {
            throw new BusinessException(
                    "CHECKLIST_ITEM_UNKNOWN",
                    "Pièce inconnue pour ce type de service",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        DossierChecklistStatutEntity entity = statutRepository
                .findByDossierIdAndOrganisationIdAndCode(dossierId, organisationId, code)
                .orElseGet(() -> {
                    DossierChecklistStatutEntity created = new DossierChecklistStatutEntity();
                    created.setOrganisationId(organisationId);
                    created.setDossierId(dossierId);
                    created.setCode(code);
                    return created;
                });
        entity.setValide(valide);
        statutRepository.save(entity);
        return list(dossierId, organisationId);
    }

    private DossierEntity requireDossier(UUID dossierId, UUID organisationId) {
        return dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
    }

    private TypeServiceEntity requireTypeService(UUID typeServiceId, UUID organisationId) {
        return typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "TYPE_SERVICE_NOT_FOUND", "Type de service introuvable", HttpStatus.NOT_FOUND));
    }

    private static boolean templateContains(JsonNode checklist, String code) {
        for (JsonNode node : templateItems(checklist)) {
            if (code.equals(node.path("code").asText(null))) {
                return true;
            }
        }
        return false;
    }

    private static Iterable<JsonNode> templateItems(JsonNode checklist) {
        if (checklist == null || !checklist.isArray()) {
            return List.of();
        }
        return checklist;
    }
}
