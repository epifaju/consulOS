package com.consulos.rendezvous;

import com.consulos.common.BusinessException;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.rendezvous.dto.RdvCreateDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RdvServiceTest {

    @Mock
    private RendezvousRepository rendezvousRepository;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private com.consulos.dossier.DossierRepository dossierRepository;
    @Mock
    private com.consulos.auth.repository.UserRepository userRepository;
    @Mock
    private DisponibiliteService disponibiliteService;
    @Mock
    private RdvStatutEngine rdvStatutEngine;
    @Mock
    private RdvNotificationService notificationService;

    @InjectMocks
    private RdvService rdvService;

    private UUID organisationId;
    private UUID usagerId;
    private OffsetDateTime dateHeure;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        dateHeure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void create_availableSlot_shouldSucceedAndNotify() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(disponibiliteService.isCreneauDisponible(eq(organisationId), eq(null), eq(dateHeure), eq(30)))
                .thenReturn(true);
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> {
            RendezvousEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        var result = rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, null, null, null),
                organisationId
        );

        assertThat(result.statut()).isEqualTo(RdvStatut.CONFIRME);
        verify(notificationService).sendConfirmation(any(RendezvousEntity.class));
    }

    @Test
    void create_fullSlot_shouldThrowConflict() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(disponibiliteService.isCreneauDisponible(eq(organisationId), eq(null), eq(dateHeure), eq(30)))
                .thenReturn(false);

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, null, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CRENEAU_COMPLET")
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void updateStatut_confirmeToPresent_shouldSucceed() {
        UUID rdvId = UUID.randomUUID();
        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(rdvId);
        rdv.setOrganisationId(organisationId);
        rdv.setUsagerId(usagerId);
        rdv.setStatut(RdvStatut.CONFIRME);
        rdv.setDateHeure(dateHeure);

        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Test");
        usager.setPrenom("User");

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.updateStatut(rdvId, RdvStatut.PRESENT, null, organisationId);

        assertThat(result.statut()).isEqualTo(RdvStatut.PRESENT);
    }

    @Test
    void updateStatut_annuleToPresent_shouldThrow() {
        UUID rdvId = UUID.randomUUID();
        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(rdvId);
        rdv.setOrganisationId(organisationId);
        rdv.setUsagerId(usagerId);
        rdv.setStatut(RdvStatut.ANNULE);

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        org.mockito.Mockito.doThrow(new BusinessException(
                "TRANSITION_INVALIDE", "Transition refusée", HttpStatus.BAD_REQUEST))
                .when(rdvStatutEngine).validateTransition(RdvStatut.ANNULE, RdvStatut.PRESENT);

        assertThatThrownBy(() -> rdvService.updateStatut(rdvId, RdvStatut.PRESENT, null, organisationId))
                .isInstanceOf(BusinessException.class);
    }
}
