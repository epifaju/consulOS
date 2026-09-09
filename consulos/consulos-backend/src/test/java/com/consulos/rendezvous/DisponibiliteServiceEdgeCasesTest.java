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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Écarts : isCreneauDisponible + filtre typeService + merge de quotas.
 */
@ExtendWith(MockitoExtension.class)
class DisponibiliteServiceEdgeCasesTest {

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
    void getDisponibilites_shouldKeepGenericAndMatchingTypeConfigsOnly() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(10);
        int jour = date.getDayOfWeek().getValue();

        ConfigCreneauEntity matching = config(null, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, 5);
        matching.setTypeServiceId(typeServiceId);
        ConfigCreneauEntity otherType = config(UUID.randomUUID(), LocalTime.of(11, 0), LocalTime.of(12, 0), 30, 5);
        ConfigCreneauEntity generic = config(null, LocalTime.of(14, 0), LocalTime.of(15, 0), 30, 3);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(organisationId, jour))
                .thenReturn(List.of(matching, otherType, generic));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(0L);

        List<CreneauDTO> slots = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        assertThat(slots).extracting(CreneauDTO::heureDebut)
                .contains(LocalTime.of(9, 0), LocalTime.of(14, 0))
                .doesNotContain(LocalTime.of(11, 0));
    }

    @Test
    void isCreneauDisponible_whenSlotPresent_shouldReturnTrue() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(12);
        int jour = date.getDayOfWeek().getValue();
        ConfigCreneauEntity config = config(null, LocalTime.of(10, 0), LocalTime.of(11, 0), 30, 4);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(organisationId, jour))
                .thenReturn(List.of(config));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(1L);

        OffsetDateTime dateHeure = date.atTime(LocalTime.of(10, 0)).atOffset(ZoneOffset.UTC);
        assertThat(disponibiliteService.isCreneauDisponible(organisationId, null, dateHeure, 30)).isTrue();
    }

    @Test
    void isCreneauDisponible_whenNoMatchingSlot_shouldReturnFalse() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(12);
        int jour = date.getDayOfWeek().getValue();
        ConfigCreneauEntity config = config(null, LocalTime.of(10, 0), LocalTime.of(11, 0), 30, 4);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(organisationId, jour))
                .thenReturn(List.of(config));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(0L);

        OffsetDateTime dateHeure = date.atTime(LocalTime.of(15, 0)).atOffset(ZoneOffset.UTC);
        assertThat(disponibiliteService.isCreneauDisponible(organisationId, null, dateHeure, 30)).isFalse();
    }

    @Test
    void getDisponibilites_whenNoConfig_shouldReturnEmpty() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(5);
        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(
                organisationId, date.getDayOfWeek().getValue())).thenReturn(List.of());

        assertThat(disponibiliteService.getDisponibilites(organisationId, typeServiceId, date)).isEmpty();
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
