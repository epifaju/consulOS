package com.consulos.dashboard.regional;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.common.BusinessException;
import com.consulos.dashboard.regional.dto.RegionalPosteKpiDTO;
import com.consulos.dashboard.regional.dto.RegionalStatsDTO;
import com.consulos.reseau.ReseauOrganisationLookupRepository;
import com.consulos.reseau.ReseauSecurityUtils;
import com.consulos.reseau.ReseauService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalDashboardService {

    private final RegionalDashboardQueryRepository repository;
    private final ReseauService reseauService;
    private final ReseauOrganisationLookupRepository organisationLookupRepository;

    @Transactional(readOnly = true)
    public RegionalStatsDTO getStats(UUID reseauId, LocalDate dateDebut, LocalDate dateFin) {
        UUID userId = ReseauSecurityUtils.getUserId();
        Set<UUID> orgIds = resolveScope(reseauId, userId);
        LocalDate today = LocalDate.now();
        LocalDate debut = dateDebut != null ? dateDebut : today.withDayOfMonth(1);
        LocalDate fin = dateFin != null ? dateFin : today;

        long dossiersOuverts = repository.countDossiersByStatuts(
                orgIds, List.of("BROUILLON", "SOUMIS", "INCOMPLET", "EN_REVUE", "VALIDE"));
        long enRevue = repository.countDossiersByStatuts(orgIds, List.of("EN_REVUE"));
        long enRetard = repository.countDossiersEnRetard(orgIds);
        long rdvJour = sumRdvTotal(orgIds, today);
        BigDecimal recettesJour = repository.sumRecettesJour(orgIds, today);
        BigDecimal recettesPeriode = repository.sumRecettesPeriode(orgIds, debut, fin);

        return new RegionalStatsDTO(
                reseauId,
                orgIds.size(),
                dossiersOuverts,
                enRevue,
                rdvJour,
                enRetard,
                recettesJour,
                recettesPeriode
        );
    }

    @Transactional(readOnly = true)
    public List<RegionalPosteKpiDTO> listPostesKpis(UUID reseauId) {
        UUID userId = ReseauSecurityUtils.getUserId();
        Set<UUID> orgIds = resolveScope(reseauId, userId);
        return loadPostesKpis(orgIds);
    }

    @Transactional(readOnly = true)
    public RegionalPosteKpiDTO getPosteStats(UUID organisationId, UUID reseauId) {
        UUID userId = ReseauSecurityUtils.getUserId();
        Set<UUID> orgIds = resolveScope(reseauId, userId);
        if (!orgIds.contains(organisationId)) {
            throw new BusinessException(
                    "POSTE_NOT_IN_SCOPE", "Poste hors périmètre réseau", HttpStatus.NOT_FOUND);
        }
        List<RegionalPosteKpiDTO> list = loadPostesKpis(Set.of(organisationId));
        if (list.isEmpty()) {
            OrganisationEntity org = organisationLookupRepository.findById(organisationId)
                    .orElseThrow(() -> new BusinessException(
                            "POSTE_NOT_FOUND", "Poste introuvable", HttpStatus.NOT_FOUND));
            return new RegionalPosteKpiDTO(
                    org.getId(), org.getCode(), org.getNom(), 0, 0, 0, BigDecimal.ZERO);
        }
        return list.getFirst();
    }

    private List<RegionalPosteKpiDTO> loadPostesKpis(Set<UUID> orgIds) {
        LocalDate today = LocalDate.now();
        OffsetDateTime debut = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<RegionalPosteKpiDTO> result = new ArrayList<>();
        for (Object[] row : repository.postesKpis(orgIds, debut, fin, today)) {
            result.add(new RegionalPosteKpiDTO(
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).longValue(),
                    new BigDecimal(row[6].toString())
            ));
        }
        return result;
    }

    private long sumRdvTotal(Set<UUID> orgIds, LocalDate date) {
        OffsetDateTime debut = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        long total = 0;
        for (Object[] row : repository.rdvStatsForDay(orgIds, debut, fin)) {
            total += ((Number) row[1]).longValue();
        }
        return total;
    }

    private Set<UUID> resolveScope(UUID reseauId, UUID userId) {
        if (reseauId != null) {
            return reseauService.resolveAllowedOrganisationIds(reseauId, userId);
        }
        Set<UUID> all = reseauService.resolveAllAllowedOrganisationIds(userId);
        return all.isEmpty() ? Set.of() : new LinkedHashSet<>(all);
    }
}
