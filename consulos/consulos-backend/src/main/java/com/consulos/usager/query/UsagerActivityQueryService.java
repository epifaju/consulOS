package com.consulos.usager.query;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.rendezvous.RendezvousEntity;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.query.dto.UsagerActivityItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsagerActivityQueryService {

    private final UsagerRepository usagerRepository;
    private final DossierRepository dossierRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final UsagerActivityRelatedDataRepository relatedDataRepository;

    @Transactional(readOnly = true)
    public List<UsagerActivityItemDTO> listActivity(UUID usagerId, UUID organisationId) {
        assertUsagerInOrganisation(usagerId, organisationId);

        List<UsagerActivityItemDTO> items = new ArrayList<>();

        List<DossierEntity> dossiers = dossierRepository
                .findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypes(dossiers);

        for (DossierEntity dossier : dossiers) {
            TypeServiceEntity type = types.get(dossier.getTypeServiceId());
            String typeLabel = type != null ? type.getLibelle() : "Prestation";
            items.add(new UsagerActivityItemDTO(
                    dossier.getCreatedAt() != null
                            ? dossier.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                            : null,
                    "DOSSIER",
                    "Dossier " + dossier.getReference(),
                    typeLabel + " — " + dossier.getStatut(),
                    dossier.getId()
            ));
        }

        for (RendezvousEntity rdv : relatedDataRepository.findRendezvousByUsager(organisationId, usagerId)) {
            items.add(new UsagerActivityItemDTO(
                    rdv.getDateHeure(),
                    "RDV",
                    "Rendez-vous",
                    (rdv.getStatut() != null ? rdv.getStatut().name() : "PLANIFIE")
                            + (rdv.getMotif() != null ? " — " + rdv.getMotif() : ""),
                    rdv.getId()
            ));
        }

        for (EncaissementEntity enc : relatedDataRepository.findEncaissementsByUsager(organisationId, usagerId)) {
            String montant = enc.getMontantTotal() != null ? enc.getMontantTotal().toPlainString() : "—";
            items.add(new UsagerActivityItemDTO(
                    enc.getCreatedAt(),
                    "ENCAISSEMENT",
                    enc.getNumeroRecu() != null ? enc.getNumeroRecu() : "Encaissement",
                    montant + " " + (enc.getDevise() != null ? enc.getDevise() : "EUR")
                            + " — " + (enc.getStatut() != null ? enc.getStatut().name() : ""),
                    enc.getId()
            ));
        }

        return items.stream()
                .sorted(Comparator.comparing(UsagerActivityItemDTO::occurredAt).reversed())
                .toList();
    }

    private void assertUsagerInOrganisation(UUID usagerId, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(usagerId)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
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
