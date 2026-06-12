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

@ExtendWith(MockitoExtension.class)
class DisponibiliteServiceTest {

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
    void getDisponibilites_normalDate_shouldReturnFreeSlots() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(7);
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }

        ConfigCreneauEntity config = new ConfigCreneauEntity();
        config.setHeureDebut(LocalTime.of(9, 0));
        config.setHeureFin(LocalTime.of(10, 0));
        config.setDureeMinutes(30);
        config.setQuotaMax(2);
        config.setActif(true);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date))
                .thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(
                organisationId, date.getDayOfWeek().getValue()))
                .thenReturn(List.of(config));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(0L);

        List<CreneauDTO> creneaux = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        assertThat(creneaux).hasSize(2);
        assertThat(creneaux.get(0).placesRestantes()).isEqualTo(2);
    }

    @Test
    void getDisponibilites_fullQuota_shouldExcludeSlot() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(7);
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }

        ConfigCreneauEntity config = new ConfigCreneauEntity();
        config.setHeureDebut(LocalTime.of(9, 0));
        config.setHeureFin(LocalTime.of(10, 0));
        config.setDureeMinutes(30);
        config.setQuotaMax(1);
        config.setActif(true);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date))
                .thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(
                organisationId, date.getDayOfWeek().getValue()))
                .thenReturn(List.of(config));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any()))
                .thenReturn(1L, 0L);

        List<CreneauDTO> creneaux = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        assertThat(creneaux).hasSize(1);
        assertThat(creneaux.get(0).heureDebut()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void getDisponibilites_ferie_shouldReturnEmpty() {
        LocalDate date = LocalDate.of(2024, 12, 25);
        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date))
                .thenReturn(true);

        List<CreneauDTO> creneaux = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        assertThat(creneaux).isEmpty();
    }

    @Test
    void getDisponibilites_pastSlots_shouldBeExcluded() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        if (date.getDayOfWeek().getValue() > 5) {
            return;
        }

        ConfigCreneauEntity config = new ConfigCreneauEntity();
        config.setHeureDebut(LocalTime.of(0, 0));
        config.setHeureFin(LocalTime.of(23, 30));
        config.setDureeMinutes(30);
        config.setQuotaMax(5);
        config.setActif(true);

        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date))
                .thenReturn(false);
        when(configCreneauRepository.findByOrganisationIdAndJourSemaineAndActifTrue(
                organisationId, date.getDayOfWeek().getValue()))
                .thenReturn(List.of(config));
        when(rendezvousRepository.countActiveInSlot(eq(organisationId), any(), any())).thenReturn(0L);

        List<CreneauDTO> creneaux = disponibiliteService.getDisponibilites(organisationId, typeServiceId, date);

        OffsetDateTime minAllowed = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
        assertThat(creneaux).allSatisfy(c ->
                assertThat(date.atTime(c.heureDebut()).atOffset(ZoneOffset.UTC)).isAfterOrEqualTo(minAllowed));
    }
}
