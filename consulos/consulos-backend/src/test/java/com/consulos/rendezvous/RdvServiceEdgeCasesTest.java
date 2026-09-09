package com.consulos.rendezvous;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.rendezvous.dto.RdvCreateDTO;
import com.consulos.rendezvous.dto.RdvUpdateDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Écarts non couverts par RdvServiceTest : agent busy, update, findById, list, agenda, annulation.
 */
@ExtendWith(MockitoExtension.class)
class RdvServiceEdgeCasesTest {

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
    private UUID rdvId;
    private OffsetDateTime dateHeure;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        rdvId = UUID.randomUUID();
        dateHeure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void create_whenAgentBusy_shouldThrowAgentIndisponible() {
        UsagerEntity usager = activeUsager();
        UserEntity agent = activeAgent();

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(disponibiliteService.isCreneauDisponible(eq(organisationId), eq(null), eq(dateHeure), eq(30)))
                .thenReturn(true);
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(rendezvousRepository.existsByAgentIdAndDateHeureAndStatutNotIn(agentId, dateHeure))
                .thenReturn(true);

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, agentId, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AGENT_INDISPONIBLE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void update_agentAndNotes_shouldSucceed() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();
        UserEntity agent = activeAgent();

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.update(rdvId, new RdvUpdateDTO(agentId, "notes maj"), organisationId);

        assertThat(result.agentId()).isEqualTo(agentId);
        assertThat(result.agentNom()).isEqualTo("Diallo Fatou");
        assertThat(result.notes()).isEqualTo("notes maj");
    }

    @Test
    void findById_success_shouldReturnDetail() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.findById(rdvId, organisationId);

        assertThat(result.id()).isEqualTo(rdvId);
        assertThat(result.usagerNom()).isEqualTo("Camara");
        assertThat(result.usagerPrenom()).isEqualTo("Aminata");
        assertThat(result.statut()).isEqualTo(RdvStatut.CONFIRME);
    }

    @Test
    void findById_wrongOrg_shouldThrowRdvNotFound() {
        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, otherOrganisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rdvService.findById(rdvId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "RDV_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void list_shouldMapUsagerNom() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();
        Pageable pageable = PageRequest.of(0, 20);

        when(rendezvousRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(rdv), pageable, 1));
        when(usagerRepository.findAllById(List.of(usagerId))).thenReturn(List.of(usager));

        var page = rdvService.list(organisationId, null, null, null, null, null, null, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().usagerNom()).isEqualTo("Camara");
        assertThat(page.content().getFirst().usagerPrenom()).isEqualTo("Aminata");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void getAgenda_shouldCallFindAgendaWithUtcOffsets() {
        LocalDate dateDebut = LocalDate.of(2026, 6, 10);
        LocalDate dateFin = LocalDate.of(2026, 6, 12);
        OffsetDateTime expectedDebut = dateDebut.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedFin = dateFin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        when(rendezvousRepository.findAgenda(organisationId, expectedDebut, expectedFin, null))
                .thenReturn(List.of());

        var agenda = rdvService.getAgenda(organisationId, dateDebut, dateFin, null);

        assertThat(agenda).isEmpty();
        verify(rendezvousRepository).findAgenda(organisationId, expectedDebut, expectedFin, null);
    }

    @Test
    void updateStatut_annule_shouldSendAnnulation() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();
        String motif = "indisponible";

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.updateStatut(rdvId, RdvStatut.ANNULE, motif, organisationId);

        assertThat(result.statut()).isEqualTo(RdvStatut.ANNULE);
        assertThat(result.motif()).isEqualTo(motif);
        verify(notificationService).sendAnnulation(any(RendezvousEntity.class), eq(motif));
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

    private UserEntity activeAgent() {
        UserEntity agent = new UserEntity();
        agent.setId(agentId);
        agent.setOrganisationId(organisationId);
        agent.setActif(true);
        agent.setNom("Diallo");
        agent.setPrenom("Fatou");
        return agent;
    }

    private RendezvousEntity baseRdv() {
        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(rdvId);
        rdv.setOrganisationId(organisationId);
        rdv.setUsagerId(usagerId);
        rdv.setDateHeure(dateHeure);
        rdv.setDureeMinutes(30);
        rdv.setStatut(RdvStatut.CONFIRME);
        return rdv;
    }
}
