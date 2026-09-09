package com.consulos.rendezvous;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RdvServiceCreateValidationTest {

    @Mock
    private RendezvousRepository rendezvousRepository;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DisponibiliteService disponibiliteService;
    @Mock
    private RdvStatutEngine rdvStatutEngine;
    @Mock
    private RdvNotificationService notificationService;

    @InjectMocks
    private RdvService rdvService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID usagerId;
    private UUID agentId;
    private UUID typeServiceId;
    private UUID dossierId;
    private OffsetDateTime dateHeure;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        dateHeure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void create_whenUsagerWrongOrgOrInactive_shouldThrowUsagerNotFound() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(otherOrganisationId);
        usager.setActif(true);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, null, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(rendezvousRepository, never()).save(any());
    }

    @Test
    void create_whenTypeServiceWrongOrg_shouldThrowTypeServiceNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, typeServiceId, null, null, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("TYPE_SERVICE_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(rendezvousRepository, never()).save(any());
    }

    @Test
    void create_whenAgentWrongOrgOrInactive_shouldThrowAgentNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(disponibiliteService.isCreneauDisponible(eq(organisationId), eq(null), eq(dateHeure), eq(30)))
                .thenReturn(true);
        UserEntity agent = new UserEntity();
        agent.setId(agentId);
        agent.setOrganisationId(otherOrganisationId);
        agent.setActif(true);
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, agentId, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("AGENT_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(rendezvousRepository, never()).save(any());
    }

    @Test
    void create_whenDossierWrongOrg_shouldThrow() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, dossierId, null, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(rendezvousRepository, never()).save(any());
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        return usager;
    }
}
