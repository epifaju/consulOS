package com.consulos.rendezvous;

import com.consulos.rendezvous.dto.CreneauDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private static final int MIN_AVANCE_MINUTES = 30;

    private final ConfigCreneauRepository configCreneauRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final RendezvousRepository rendezvousRepository;

    @Transactional(readOnly = true)
    public List<CreneauDTO> getDisponibilites(UUID organisationId, UUID typeServiceId, LocalDate date) {
        if (jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)) {
            return List.of();
        }

        int jourSemaine = date.getDayOfWeek().getValue();
        List<ConfigCreneauEntity> configs = configCreneauRepository
                .findByOrganisationIdAndJourSemaineAndActifTrue(organisationId, jourSemaine)
                .stream()
                .filter(c -> c.getTypeServiceId() == null || c.getTypeServiceId().equals(typeServiceId))
                .toList();

        if (configs.isEmpty()) {
            return List.of();
        }

        Map<LocalTime, CreneauDTO> creneauxMap = new LinkedHashMap<>();
        OffsetDateTime minAllowed = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(MIN_AVANCE_MINUTES);

        for (ConfigCreneauEntity config : configs) {
            int duree = config.getDureeMinutes() != null ? config.getDureeMinutes() : 30;
            int quota = config.getQuotaMax() != null ? config.getQuotaMax() : 10;
            int startMinutes = config.getHeureDebut().toSecondOfDay() / 60;
            int endMinutes = config.getHeureFin().toSecondOfDay() / 60;

            for (int slotStartMinutes = startMinutes; slotStartMinutes + duree <= endMinutes; slotStartMinutes += duree) {
                LocalTime current = LocalTime.of(slotStartMinutes / 60, slotStartMinutes % 60);
                LocalTime fin = current.plusMinutes(duree);
                OffsetDateTime slotStart = date.atTime(current).atOffset(ZoneOffset.UTC);
                OffsetDateTime slotEnd = date.atTime(fin).atOffset(ZoneOffset.UTC);

                if (slotStart.isBefore(minAllowed)) {
                    continue;
                }

                long count = rendezvousRepository.countActiveInSlot(organisationId, slotStart, slotEnd);
                int placesRestantes = quota - (int) count;

                if (placesRestantes > 0) {
                    creneauxMap.merge(
                            current,
                            new CreneauDTO(current, fin, placesRestantes, quota),
                            (existing, incoming) -> incoming.placesRestantes() > existing.placesRestantes()
                                    ? incoming
                                    : existing
                    );
                }
            }
        }

        return creneauxMap.values().stream()
                .sorted(Comparator.comparing(CreneauDTO::heureDebut))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isCreneauDisponible(
            UUID organisationId,
            UUID typeServiceId,
            OffsetDateTime dateHeure,
            int dureeMinutes
    ) {
        LocalDate date = dateHeure.toLocalDate();
        LocalTime heure = dateHeure.toLocalTime().withNano(0);

        return getDisponibilites(organisationId, typeServiceId, date).stream()
                .anyMatch(c -> c.heureDebut().equals(heure) && c.placesRestantes() > 0);
    }
}
