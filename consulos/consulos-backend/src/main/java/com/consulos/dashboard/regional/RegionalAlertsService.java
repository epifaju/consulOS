package com.consulos.dashboard.regional;

import com.consulos.dashboard.regional.dto.RegionalAlerteDTO;
import com.consulos.reseau.ReseauOrganisationLookupRepository;
import com.consulos.reseau.ReseauSecurityUtils;
import com.consulos.reseau.ReseauService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalAlertsService {

    private final RegionalAlertsQueryRepository repository;
    private final ReseauService reseauService;
    private final ReseauOrganisationLookupRepository organisationLookupRepository;

    @Transactional(readOnly = true)
    public List<RegionalAlerteDTO> getAlertes(UUID reseauId) {
        UUID userId = ReseauSecurityUtils.getUserId();
        Set<UUID> orgIds = resolveScope(reseauId, userId);
        if (orgIds.isEmpty()) {
            return List.of();
        }

        List<RegionalAlerteDTO> alertes = new ArrayList<>();

        List<UUID> bloques = repository.dossiersBloquesEnRevue(orgIds);
        if (!bloques.isEmpty()) {
            alertes.add(new RegionalAlerteDTO(
                    "DOSSIERS_BLOQUES",
                    "warning",
                    "Dossiers en revue depuis plus de 7 jours (réseau)",
                    bloques.size(),
                    repository.posteCodesForDossierIds(bloques),
                    bloques.size() > 50 ? bloques.subList(0, 50) : bloques
            ));
        }

        List<UUID> echeances = repository.dossiersEcheanceDepassee(orgIds);
        if (!echeances.isEmpty()) {
            alertes.add(new RegionalAlerteDTO(
                    "ECHEANCES_DEPASSEES",
                    "critical",
                    "Dossiers dont l'échéance est dépassée (réseau)",
                    echeances.size(),
                    repository.posteCodesForDossierIds(echeances),
                    echeances.size() > 50 ? echeances.subList(0, 50) : echeances
            ));
        }

        List<Object[]> surcharges = repository.agentsSurcharges(orgIds);
        if (!surcharges.isEmpty()) {
            Set<UUID> orgSurcharges = surcharges.stream()
                    .map(row -> (UUID) row[0])
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> codes = organisationLookupRepository.findByIdIn(orgSurcharges).stream()
                    .map(o -> o.getCode())
                    .sorted()
                    .toList();
            alertes.add(new RegionalAlerteDTO(
                    "AGENTS_SURCHARGES",
                    "warning",
                    "Agents avec plus de 20 dossiers en revue (réseau)",
                    surcharges.size(),
                    codes,
                    List.of()
            ));
        }

        List<Object[]> caisse = repository.postesCaisseSilencieuse(orgIds, LocalDate.now());
        if (!caisse.isEmpty()) {
            List<String> codes = caisse.stream().map(row -> String.valueOf(row[1])).toList();
            alertes.add(new RegionalAlerteDTO(
                    "CAISSE_SILENCIEUSE",
                    "warning",
                    "Postes avec dossiers ouverts mais aucune recette aujourd'hui",
                    caisse.size(),
                    codes,
                    List.of()
            ));
        }

        return alertes;
    }

    private Set<UUID> resolveScope(UUID reseauId, UUID userId) {
        if (reseauId != null) {
            return reseauService.resolveAllowedOrganisationIds(reseauId, userId);
        }
        Set<UUID> all = reseauService.resolveAllAllowedOrganisationIds(userId);
        return all.isEmpty() ? Set.of() : new LinkedHashSet<>(all);
    }
}
