package com.consulos.reporting.regional;

import com.consulos.common.BusinessException;
import com.consulos.reporting.dto.EncaissementTotauxDTO;
import com.consulos.reporting.dto.ModePaiementTotalDTO;
import com.consulos.reporting.dto.RdvTotauxDTO;
import com.consulos.reporting.dto.ServiceTotalDTO;
import com.consulos.reporting.dto.StatutCountDTO;
import com.consulos.reporting.regional.dto.RegionalPosteMensuelDTO;
import com.consulos.reporting.regional.dto.RegionalReportingMensuelDTO;
import com.consulos.reseau.ReseauSecurityUtils;
import com.consulos.reseau.ReseauService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class RegionalReportingService {

    private final RegionalReportingQueryRepository queryRepository;
    private final ReseauService reseauService;

    @Transactional(readOnly = true)
    public RegionalReportingMensuelDTO mensuel(int annee, int mois, UUID reseauId) {
        PeriodBounds period = resolvePeriod(annee, mois);
        UUID userId = ReseauSecurityUtils.getUserId();
        Set<UUID> orgIds = resolveScope(reseauId, userId);

        List<StatutCountDTO> dossiersParStatut = mapStatutCounts(
                queryRepository.dossiersCreesParStatut(orgIds, period.debut(), period.fin()));
        long dossiersCrees = queryRepository.countDossiersCrees(orgIds, period.debut(), period.fin());

        Object[] encTotaux = queryRepository.encaissementTotaux(orgIds, period.debut(), period.fin());
        BigDecimal montantTotal = toBigDecimal(encTotaux[0]);
        long nbEnc = ((Number) encTotaux[1]).longValue();

        List<ModePaiementTotalDTO> parMode = new ArrayList<>();
        for (Object[] row : queryRepository.encaissementsParMode(orgIds, period.debut(), period.fin())) {
            parMode.add(new ModePaiementTotalDTO(
                    String.valueOf(row[0]),
                    ((Number) row[1]).longValue(),
                    toBigDecimal(row[2])
            ));
        }

        List<ServiceTotalDTO> parService = new ArrayList<>();
        for (Object[] row : queryRepository.encaissementsParService(orgIds, period.debut(), period.fin())) {
            parService.add(new ServiceTotalDTO(
                    String.valueOf(row[0]),
                    toBigDecimal(row[1]),
                    ((Number) row[2]).longValue()
            ));
        }

        List<StatutCountDTO> rdvParStatut = mapStatutCounts(
                queryRepository.rdvParStatut(orgIds, period.debut(), period.fin()));
        long rdvTotal = rdvParStatut.stream().mapToLong(StatutCountDTO::count).sum();

        long documents = queryRepository.countDocuments(orgIds, period.debut(), period.fin());
        BigDecimal delai = queryRepository.avgDelaiHeures(orgIds, period.debut(), period.fin())
                .setScale(2, RoundingMode.HALF_UP);

        List<RegionalPosteMensuelDTO> parPoste = new ArrayList<>();
        for (Object[] row : queryRepository.postesMensuel(orgIds, period.debut(), period.fin())) {
            parPoste.add(new RegionalPosteMensuelDTO(
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue(),
                    toBigDecimal(row[4]),
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue()
            ));
        }

        return new RegionalReportingMensuelDTO(
                reseauId,
                orgIds.size(),
                annee,
                mois,
                period.dateDebut(),
                period.dateFin(),
                dossiersCrees,
                dossiersParStatut,
                new EncaissementTotauxDTO(montantTotal, nbEnc, parMode, parService),
                new RdvTotauxDTO(rdvTotal, rdvParStatut),
                documents,
                delai,
                parPoste
        );
    }

    private Set<UUID> resolveScope(UUID reseauId, UUID userId) {
        if (reseauId != null) {
            return reseauService.resolveAllowedOrganisationIds(reseauId, userId);
        }
        Set<UUID> all = reseauService.resolveAllAllowedOrganisationIds(userId);
        return all.isEmpty() ? Set.of() : new LinkedHashSet<>(all);
    }

    static PeriodBounds resolvePeriod(int annee, int mois) {
        if (mois < 1 || mois > 12) {
            throw new BusinessException("INVALID_MONTH", "Le mois doit être entre 1 et 12", HttpStatus.BAD_REQUEST);
        }
        if (annee < 2000 || annee > 2100) {
            throw new BusinessException("INVALID_YEAR", "Année invalide", HttpStatus.BAD_REQUEST);
        }
        YearMonth ym = YearMonth.of(annee, mois);
        LocalDate dateDebut = ym.atDay(1);
        LocalDate dateFin = ym.atEndOfMonth();
        OffsetDateTime debut = dateDebut.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = dateFin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return new PeriodBounds(dateDebut, dateFin, debut, fin);
    }

    private static List<StatutCountDTO> mapStatutCounts(List<Object[]> rows) {
        List<StatutCountDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new StatutCountDTO(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        }
        return result;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    record PeriodBounds(LocalDate dateDebut, LocalDate dateFin, OffsetDateTime debut, OffsetDateTime fin) {
    }
}
