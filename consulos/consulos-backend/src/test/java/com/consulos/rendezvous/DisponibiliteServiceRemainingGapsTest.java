package com.consulos.rendezvous;

import com.consulos.rendezvous.dto.CreneauDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Gaps: merge overlapping configs (higher placesRestantes wins) + null duree/quota defaults.
 */
@ExtendWith(MockitoExtension.class)
class DisponibiliteServiceRemainingGapsTest {

    @Mock
    private ConfigCreneauRepository configCreneauRepository;
    @Mock
    private JourFermetureRepository jourFermetureRepository;
    @Mock
    private RendezvousRepository rendezvousRepository;

    @InjectMocks
    private DisponibiliteService disponibiliteService;

    private UUID organisationId;
    private UUID typeServiceId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
    }

    @Test
    void getDisponibilites_overlappingConfigs_shouldKeepHigherPlacesRestantes() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(14);
        int jour = date.getDayOfWeek().getValue();

        ConfigCreneauEntity lowQuota = config(null, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, 3);
        ConfigCreneauEntity highQuota = config(null, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, 8);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(organisationId, jour))
                .thenReturn(List.of(lowQuota, highQuota));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(1L);

        List<CreneauDTO> slots = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        CreneauDTO slot9 = slots.stream()
                .filter(c -> c.heureDebut().equals(LocalTime.of(9, 0)))
                .findFirst()
                .orElseThrow();
        assertThat(slot9.placesRestantes()).isEqualTo(7);
        assertThat(slot9.quotaMax()).isEqualTo(8);
    }

    @Test
    void getDisponibilites_nullDureeAndQuota_shouldDefaultTo30And10() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(15);
        int jour = date.getDayOfWeek().getValue();

        ConfigCreneauEntity config = new ConfigCreneauEntity();
        config.setHeureDebut(LocalTime.of(10, 0));
        config.setHeureFin(LocalTime.of(11, 0));
        config.setDureeMinutes(null);
        config.setQuotaMax(null);
        config.setActif(true);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(organisationId, jour))
                .thenReturn(List.of(config));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(0L);

        List<CreneauDTO> slots = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        assertThat(slots).hasSize(2);
        assertThat(slots).extracting(CreneauDTO::heureDebut)
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(10, 30));
        assertThat(slots).allSatisfy(c -> {
            assertThat(c.quotaMax()).isEqualTo(10);
            assertThat(c.placesRestantes()).isEqualTo(10);
            assertThat(c.heureFin()).isEqualTo(c.heureDebut().plusMinutes(30));
        });
    }

    private ConfigCreneauEntity config(UUID typeId, LocalTime debut, LocalTime fin, int duree, int quota) {
        ConfigCreneauEntity entity = new ConfigCreneauEntity();
        entity.setTypeServiceId(typeId);
        entity.setHeureDebut(debut);
        entity.setHeureFin(fin);
        entity.setDureeMinutes(duree);
        entity.setQuotaMax(quota);
        entity.setActif(true);
        return entity;
    }
}
