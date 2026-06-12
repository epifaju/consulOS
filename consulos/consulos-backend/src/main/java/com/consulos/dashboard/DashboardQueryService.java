package com.consulos.dashboard;

import com.consulos.dashboard.dto.AgentChargeDTO;
import com.consulos.dashboard.dto.AlerteDTO;
import com.consulos.dashboard.dto.ChartPointDTO;
import com.consulos.dashboard.dto.DashboardStatsDTO;
import com.consulos.dashboard.dto.DossierChartDTO;
import com.consulos.dashboard.dto.DossierStatsDTO;
import com.consulos.dashboard.dto.KpiDTO;
import com.consulos.dashboard.dto.RecetteChartPointDTO;
import com.consulos.dashboard.dto.RecetteServiceDTO;
import com.consulos.dashboard.dto.RdvStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final DashboardQueryRepository repository;

    @Transactional(readOnly = true)
    public DossierStatsDTO getDossierStats(UUID organisationId, LocalDate dateDebut, LocalDate dateFin) {
        long ouverts = repository.countDossiersByStatuts(
                organisationId, List.of("BROUILLON", "SOUMIS", "INCOMPLET", "EN_REVUE", "VALIDE"));
        long enCours = repository.countDossiersByStatuts(
                organisationId, List.of("SOUMIS", "INCOMPLET", "EN_REVUE"));
        long valides = repository.countDossiersByStatuts(organisationId, List.of("VALIDE", "CLOTURE"));
        long rejetes = repository.countDossiersByStatuts(organisationId, List.of("REJETE"));
        BigDecimal delai = repository.avgDelaiHeures(organisationId, dateDebut, dateFin)
                .setScale(2, RoundingMode.HALF_UP);
        long enRetard = repository.countDossiersEnRetard(organisationId);
        return new DossierStatsDTO(ouverts, enCours, valides, rejetes, delai, enRetard);
    }

    @Transactional(readOnly = true)
    public List<ChartPointDTO> getDossiersParJour(UUID organisationId, LocalDate dateDebut, LocalDate dateFin) {
        List<ChartPointDTO> points = new ArrayList<>();
        for (Object[] row : repository.dossiersParJour(organisationId, dateDebut, dateFin)) {
            LocalDate date = toLocalDate(row[0]);
            long count = ((Number) row[1]).longValue();
            points.add(new ChartPointDTO(date, count));
        }
        return points;
    }

    @Transactional(readOnly = true)
    public List<RecetteServiceDTO> getRecettesParService(UUID organisationId, LocalDate dateDebut, LocalDate dateFin) {
        List<RecetteServiceDTO> result = new ArrayList<>();
        for (Object[] row : repository.recettesParService(organisationId, dateDebut, dateFin)) {
            result.add(new RecetteServiceDTO(
                    (String) row[0],
                    new BigDecimal(row[1].toString()),
                    ((Number) row[2]).longValue()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public RdvStatsDTO getRdvStats(UUID organisationId, LocalDate date) {
        OffsetDateTime debut = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        long total = 0;
        long confirmes = 0;
        long presents = 0;
        long absents = 0;

        for (Object[] row : repository.rdvStatsForDay(organisationId, debut, fin)) {
            String statut = row[0].toString();
            long count = ((Number) row[1]).longValue();
            total += count;
            switch (statut) {
                case "CONFIRME", "EN_ATTENTE" -> confirmes += count;
                case "PRESENT" -> presents += count;
                case "ABSENT" -> absents += count;
                default -> { }
            }
        }

        long evalues = presents + absents;
        BigDecimal taux = evalues == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(presents * 100.0 / evalues).setScale(1, RoundingMode.HALF_UP);

        return new RdvStatsDTO(total, confirmes, presents, absents, taux);
    }

    @Transactional(readOnly = true)
    public List<AlerteDTO> getAlertesActives(UUID organisationId) {
        List<AlerteDTO> alertes = new ArrayList<>();

        List<UUID> bloques = repository.dossiersBloquesEnRevue(organisationId);
        if (!bloques.isEmpty()) {
            alertes.add(new AlerteDTO(
                    "DOSSIERS_BLOQUES",
                    "warning",
                    "Dossiers en revue depuis plus de 7 jours",
                    bloques.size(),
                    bloques
            ));
        }

        List<UUID> echeances = repository.dossiersEcheanceDepassee(organisationId);
        if (!echeances.isEmpty()) {
            alertes.add(new AlerteDTO(
                    "ECHEANCES_DEPASSEES",
                    "critical",
                    "Dossiers dont l'échéance est dépassée",
                    echeances.size(),
                    echeances
            ));
        }

        List<Object[]> surcharges = repository.agentsSurcharges(organisationId);
        if (!surcharges.isEmpty()) {
            alertes.add(new AlerteDTO(
                    "AGENTS_SURCHARGES",
                    "warning",
                    "Agents avec plus de 20 dossiers en revue",
                    surcharges.size(),
                    List.of()
            ));
        }

        return alertes;
    }

    @Transactional(readOnly = true)
    public List<AgentChargeDTO> getAgentsCharge(UUID organisationId) {
        LocalDate today = LocalDate.now();
        OffsetDateTime debut = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<AgentChargeDTO> result = new ArrayList<>();
        for (Object[] row : repository.agentsCharge(organisationId, debut, fin)) {
            UUID agentId = (UUID) row[0];
            String nom = row[1] + " " + row[2];
            long nbDossiers = ((Number) row[3]).longValue();
            long nbRdv = ((Number) row[4]).longValue();
            result.add(new AgentChargeDTO(agentId, nom.trim(), nbDossiers, nbRdv));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(UUID organisationId, LocalDate dateDebut, LocalDate dateFin) {
        LocalDate today = LocalDate.now();
        long periodDays = Math.max(1, dateFin.toEpochDay() - dateDebut.toEpochDay() + 1);
        LocalDate prevFin = dateDebut.minusDays(1);
        LocalDate prevDebut = prevFin.minusDays(periodDays - 1);

        DossierStatsDTO dossierStats = getDossierStats(organisationId, dateDebut, dateFin);
        RdvStatsDTO rdvStats = getRdvStats(organisationId, today);

        long dossiersPeriode = repository.dossiersParJour(organisationId, dateDebut, dateFin).stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();
        long dossiersPeriodePrec = repository.dossiersParJour(organisationId, prevDebut, prevFin).stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        long enRevue = repository.countDossiersByStatuts(organisationId, List.of("EN_REVUE"));

        BigDecimal recettesJour = repository.sumRecettesJour(organisationId, today);
        BigDecimal recettesHier = repository.sumRecettesJour(organisationId, today.minusDays(1));
        BigDecimal recettesPeriode = getRecettesParService(organisationId, dateDebut, dateFin).stream()
                .map(RecetteServiceDTO::montantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardStatsDTO(
                buildKpi("Dossiers ouverts", dossierStats.totalOuverts(), dossierStats.totalOuverts()),
                buildKpi("En attente validation", enRevue, enRevue),
                buildKpi("RDV du jour", rdvStats.total(), rdvStats.total()),
                buildKpiFromDecimal("Recettes du jour", recettesJour, recettesHier),
                dossierStats,
                rdvStats,
                recettesPeriode
        );
    }

    @Transactional(readOnly = true)
    public List<DossierChartDTO> getDossiersChart(UUID organisationId, DashboardPeriode periode) {
        LocalDate fin = LocalDate.now();
        LocalDate debut = switch (periode) {
            case SEMAINE -> fin.minusDays(7);
            case MOIS -> fin.minusDays(30);
            case TRIMESTRE -> fin.minusDays(90);
        };
        List<DossierChartDTO> data = new ArrayList<>();
        for (Object[] row : repository.dossierChartData(organisationId, debut, fin)) {
            data.add(new DossierChartDTO((String) row[0], row[1].toString(), ((Number) row[2]).longValue()));
        }
        return data;
    }

    @Transactional(readOnly = true)
    public List<RecetteChartPointDTO> getRecettesChart(UUID organisationId, DashboardPeriode periode) {
        LocalDate fin = LocalDate.now();
        LocalDate debut = switch (periode) {
            case SEMAINE -> fin.minusDays(7);
            case MOIS -> fin.minusDays(30);
            case TRIMESTRE -> fin.minusDays(90);
        };
        List<RecetteChartPointDTO> data = new ArrayList<>();
        for (Object[] row : repository.recetteChartData(organisationId, debut, fin)) {
            data.add(new RecetteChartPointDTO(
                    toLocalDate(row[0]),
                    row[1].toString(),
                    new BigDecimal(row[2].toString())
            ));
        }
        return data;
    }

    private KpiDTO buildKpi(String label, long valeur, long valeurPrecedente) {
        return new KpiDTO(label, valeur, valeurPrecedente, computeTendance(valeur, valeurPrecedente));
    }

    private KpiDTO buildKpiFromDecimal(String label, BigDecimal valeur, BigDecimal valeurPrecedente) {
        long v = valeur.longValue();
        long vp = valeurPrecedente.longValue();
        return new KpiDTO(label, v, vp, computeTendance(v, vp));
    }

    private BigDecimal computeTendance(long valeur, long valeurPrecedente) {
        if (valeurPrecedente == 0) {
            return valeur > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((valeur - valeurPrecedente) * 100.0 / valeurPrecedente)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
