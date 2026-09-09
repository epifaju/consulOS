package com.consulos.rendezvous;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Gaps remaining after RdvServiceTest / EdgeCasesTest / CreateValidationTest.
 */
@ExtendWith(MockitoExtension.class)
class RdvServiceRemainingGapsTest {

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
    private UUID usagerId;
    private UUID agentId;
    private UUID typeServiceId;
    private UUID dossierId;
    private UUID rdvId;
    private OffsetDateTime dateHeure;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        rdvId = UUID.randomUUID();
        dateHeure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void create_withAgentFree_shouldSucceed() {
        UsagerEntity usager = activeUsager();
        UserEntity agent = activeAgent();

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(disponibiliteService.isCreneauDisponible(eq(organisationId), eq(null), eq(dateHeure), eq(30)))
                .thenReturn(true);
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(rendezvousRepository.existsByAgentIdAndDateHeureAndStatutNotIn(agentId, dateHeure))
                .thenReturn(false);
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> {
            RendezvousEntity e = inv.getArgument(0);
            e.setId(rdvId);
            return e;
        });

        var result = rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, agentId, null, null),
                organisationId
        );

        assertThat(result.id()).isEqualTo(rdvId);
        assertThat(result.agentId()).isEqualTo(agentId);
        assertThat(result.agentNom()).isEqualTo("Diallo Fatou");
        assertThat(result.statut()).isEqualTo(RdvStatut.CONFIRME);
        verify(notificationService).sendConfirmation(any(RendezvousEntity.class));
    }

    @Test
    void create_withTypeDossierAndCustomDuree_shouldSucceed() {
        UsagerEntity usager = activeUsager();
        TypeServiceEntity type = typeService();
        DossierEntity dossier = dossier();

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(type));
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(disponibiliteService.isCreneauDisponible(eq(organisationId), eq(typeServiceId), eq(dateHeure), eq(45)))
                .thenReturn(true);
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> {
            RendezvousEntity e = inv.getArgument(0);
            e.setId(rdvId);
            return e;
        });
        when(typeServiceRepository.findById(typeServiceId)).thenReturn(Optional.of(type));
        when(dossierRepository.findById(dossierId)).thenReturn(Optional.of(dossier));

        var result = rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, typeServiceId, dossierId, null, 45, "note"),
                organisationId
        );

        assertThat(result.dureeMinutes()).isEqualTo(45);
        assertThat(result.typeServiceId()).isEqualTo(typeServiceId);
        assertThat(result.typeServiceLibelle()).isEqualTo("Passeport");
        assertThat(result.dossierId()).isEqualTo(dossierId);
        assertThat(result.dossierReference()).isEqualTo("DOS-2026-00001");
        assertThat(result.notes()).isEqualTo("note");
    }

    @Test
    void create_whenUsagerMissing_shouldThrowUsagerNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rdvService.create(
                new RdvCreateDTO(usagerId, dateHeure, null, null, null, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USAGER_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(rendezvousRepository, never()).save(any());
    }

    @Test
    void updateStatut_reporte_shouldSetMotifAndSkipAnnulation() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();
        String motif = "report demandé";

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.updateStatut(rdvId, RdvStatut.REPORTE, motif, organisationId);

        assertThat(result.statut()).isEqualTo(RdvStatut.REPORTE);
        assertThat(result.motif()).isEqualTo(motif);
        verify(notificationService, never()).sendAnnulation(any(), any());
    }

    @Test
    void updateStatut_absent_shouldNotPersistMotif() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.updateStatut(rdvId, RdvStatut.ABSENT, "ne devrait pas être stocké", organisationId);

        assertThat(result.statut()).isEqualTo(RdvStatut.ABSENT);
        assertThat(result.motif()).isNull();
        assertThat(rdv.getMotif()).isNull();
        verify(notificationService, never()).sendAnnulation(any(), any());
    }

    @Test
    void update_notesOnly_shouldNotCallUserRepository() {
        RendezvousEntity rdv = baseRdv();
        UsagerEntity usager = activeUsager();

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(rendezvousRepository.save(any(RendezvousEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = rdvService.update(rdvId, new RdvUpdateDTO(null, "notes seules"), organisationId);

        assertThat(result.notes()).isEqualTo("notes seules");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void update_whenAgentNotFound_shouldThrow() {
        RendezvousEntity rdv = baseRdv();

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(userRepository.findById(agentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rdvService.update(rdvId, new RdvUpdateDTO(agentId, null), organisationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AGENT_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(rendezvousRepository, never()).save(any());
    }

    @Test
    void list_shouldMapTypeAndAgentLabels() {
        RendezvousEntity rdv = baseRdv();
        rdv.setTypeServiceId(typeServiceId);
        rdv.setAgentId(agentId);
        UsagerEntity usager = activeUsager();
        TypeServiceEntity type = typeService();
        UserEntity agent = activeAgent();
        Pageable pageable = PageRequest.of(0, 20);

        when(rendezvousRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(rdv), pageable, 1));
        when(usagerRepository.findAllById(List.of(usagerId))).thenReturn(List.of(usager));
        when(typeServiceRepository.findAllById(List.of(typeServiceId))).thenReturn(List.of(type));
        when(userRepository.findAllById(List.of(agentId))).thenReturn(List.of(agent));

        var page = rdvService.list(organisationId, null, null, null, null, null, null, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().typeServiceLibelle()).isEqualTo("Passeport");
        assertThat(page.content().getFirst().agentNom()).isEqualTo("Diallo Fatou");
    }

    @Test
    void getAgenda_withAgentFilter_shouldMapItems() {
        LocalDate dateDebut = LocalDate.of(2026, 6, 10);
        LocalDate dateFin = LocalDate.of(2026, 6, 12);
        OffsetDateTime expectedDebut = dateDebut.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedFin = dateFin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        RendezvousEntity rdv = baseRdv();
        rdv.setTypeServiceId(typeServiceId);
        rdv.setAgentId(agentId);
        UsagerEntity usager = activeUsager();
        TypeServiceEntity type = typeService();
        UserEntity agent = activeAgent();

        when(rendezvousRepository.findAgenda(organisationId, expectedDebut, expectedFin, agentId))
                .thenReturn(List.of(rdv));
        when(usagerRepository.findAllById(List.of(usagerId))).thenReturn(List.of(usager));
        when(typeServiceRepository.findAllById(List.of(typeServiceId))).thenReturn(List.of(type));
        when(userRepository.findAllById(List.of(agentId))).thenReturn(List.of(agent));

        var agenda = rdvService.getAgenda(organisationId, dateDebut, dateFin, agentId);

        assertThat(agenda).hasSize(1);
        assertThat(agenda.getFirst().id()).isEqualTo(rdvId);
        assertThat(agenda.getFirst().typeServiceLibelle()).isEqualTo("Passeport");
        assertThat(agenda.getFirst().agentNom()).isEqualTo("Diallo Fatou");
        assertThat(agenda.getFirst().usagerNom()).isEqualTo("Camara");
        verify(rendezvousRepository).findAgenda(organisationId, expectedDebut, expectedFin, agentId);
    }

    @Test
    void findById_shouldEnrichTypeAgentDossier() {
        RendezvousEntity rdv = baseRdv();
        rdv.setTypeServiceId(typeServiceId);
        rdv.setAgentId(agentId);
        rdv.setDossierId(dossierId);
        UsagerEntity usager = activeUsager();
        TypeServiceEntity type = typeService();
        UserEntity agent = activeAgent();
        DossierEntity dossier = dossier();

        when(rendezvousRepository.findByIdAndOrganisationId(rdvId, organisationId)).thenReturn(Optional.of(rdv));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findById(typeServiceId)).thenReturn(Optional.of(type));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(dossierRepository.findById(dossierId)).thenReturn(Optional.of(dossier));

        var result = rdvService.findById(rdvId, organisationId);

        assertThat(result.typeServiceLibelle()).isEqualTo("Passeport");
        assertThat(result.agentNom()).isEqualTo("Diallo Fatou");
        assertThat(result.dossierReference()).isEqualTo("DOS-2026-00001");
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        usager.setEmail("aminata@example.com");
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

    private TypeServiceEntity typeService() {
        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setOrganisationId(organisationId);
        type.setLibelle("Passeport");
        type.setActif(true);
        return type;
    }

    private DossierEntity dossier() {
        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setReference("DOS-2026-00001");
        dossier.setActif(true);
        return dossier;
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
