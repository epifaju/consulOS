package com.consulos.rendezvous;

import com.consulos.common.BusinessException;
import com.consulos.rendezvous.dto.ConfigCreneauDTO;
import com.consulos.rendezvous.dto.JourFermetureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametresServiceTest {

    @Mock
    private ConfigCreneauRepository configCreneauRepository;
    @Mock
    private JourFermetureRepository jourFermetureRepository;

    @InjectMocks
    private ParametresService parametresService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID creneauId;
    private UUID fermetureId;
    private UUID typeServiceId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        creneauId = UUID.randomUUID();
        fermetureId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
    }

    @Test
    void listCreneaux_shouldMapEntities() {
        ConfigCreneauEntity entity = buildCreneau();
        when(configCreneauRepository.findByOrganisationId(organisationId)).thenReturn(List.of(entity));

        List<ConfigCreneauDTO> result = parametresService.listCreneaux(organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().jourSemaine()).isEqualTo(2);
        assertThat(result.getFirst().quotaMax()).isEqualTo(8);
    }

    @Test
    void createCreneau_shouldApplyDefaults() {
        ConfigCreneauDTO dto = new ConfigCreneauDTO(
                null, typeServiceId, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), null, null, null
        );
        when(configCreneauRepository.save(any(ConfigCreneauEntity.class))).thenAnswer(invocation -> {
            ConfigCreneauEntity saved = invocation.getArgument(0);
            saved.setId(creneauId);
            return saved;
        });

        ConfigCreneauDTO result = parametresService.createCreneau(dto, organisationId);

        ArgumentCaptor<ConfigCreneauEntity> captor = ArgumentCaptor.forClass(ConfigCreneauEntity.class);
        verify(configCreneauRepository).save(captor.capture());
        ConfigCreneauEntity saved = captor.getValue();
        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getDureeMinutes()).isEqualTo(30);
        assertThat(saved.getQuotaMax()).isEqualTo(10);
        assertThat(saved.getActif()).isTrue();
        assertThat(result.id()).isEqualTo(creneauId);
    }

    @Test
    void updateCreneau_whenOtherOrganisation_shouldThrowNotFound() {
        ConfigCreneauEntity entity = buildCreneau();
        entity.setOrganisationId(otherOrganisationId);
        when(configCreneauRepository.findById(creneauId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> parametresService.updateCreneau(
                creneauId,
                new ConfigCreneauDTO(creneauId, typeServiceId, 3, LocalTime.of(10, 0), LocalTime.of(11, 0), 20, 5, true),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("CRENEAU_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(configCreneauRepository, never()).save(any());
    }

    @Test
    void updateCreneau_shouldPersistChanges() {
        ConfigCreneauEntity entity = buildCreneau();
        when(configCreneauRepository.findById(creneauId)).thenReturn(Optional.of(entity));
        when(configCreneauRepository.save(any(ConfigCreneauEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigCreneauDTO result = parametresService.updateCreneau(
                creneauId,
                new ConfigCreneauDTO(creneauId, typeServiceId, 5, LocalTime.of(14, 0), LocalTime.of(17, 0), 45, 12, false),
                organisationId
        );

        assertThat(entity.getJourSemaine()).isEqualTo(5);
        assertThat(entity.getHeureDebut()).isEqualTo(LocalTime.of(14, 0));
        assertThat(entity.getDureeMinutes()).isEqualTo(45);
        assertThat(entity.getQuotaMax()).isEqualTo(12);
        assertThat(entity.getActif()).isFalse();
        assertThat(result.actif()).isFalse();
    }

    @Test
    void createJourFermeture_shouldPersist() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(false);
        when(jourFermetureRepository.save(any(JourFermetureEntity.class))).thenAnswer(invocation -> {
            JourFermetureEntity saved = invocation.getArgument(0);
            saved.setId(fermetureId);
            return saved;
        });

        JourFermetureDTO result = parametresService.createJourFermeture(
                new JourFermetureDTO(null, date, "Noël"),
                organisationId
        );

        assertThat(result.id()).isEqualTo(fermetureId);
        assertThat(result.motif()).isEqualTo("Noël");
        ArgumentCaptor<JourFermetureEntity> captor = ArgumentCaptor.forClass(JourFermetureEntity.class);
        verify(jourFermetureRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganisationId()).isEqualTo(organisationId);
    }

    @Test
    void createJourFermeture_whenDateExists_shouldThrowConflict() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        when(jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, date)).thenReturn(true);

        assertThatThrownBy(() -> parametresService.createJourFermeture(
                new JourFermetureDTO(null, date, "Nouvel An"),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("FERMETURE_EXISTANTE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void updateJourFermeture_whenMissing_shouldThrowNotFound() {
        when(jourFermetureRepository.findByIdAndOrganisationId(fermetureId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> parametresService.updateJourFermeture(
                fermetureId,
                new JourFermetureDTO(fermetureId, LocalDate.of(2026, 5, 1), "Fête"),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("FERMETURE_NOT_FOUND"));
    }

    @Test
    void listJoursFermeture_shouldReturnOrderedDtos() {
        JourFermetureEntity entity = new JourFermetureEntity();
        entity.setId(fermetureId);
        entity.setOrganisationId(organisationId);
        entity.setDateFermeture(LocalDate.of(2026, 7, 14));
        entity.setMotif("Fête nationale");
        when(jourFermetureRepository.findByOrganisationIdOrderByDateFermetureAsc(organisationId))
                .thenReturn(List.of(entity));

        List<JourFermetureDTO> result = parametresService.listJoursFermeture(organisationId);

        assertThat(result).containsExactly(new JourFermetureDTO(fermetureId, LocalDate.of(2026, 7, 14), "Fête nationale"));
    }

    private ConfigCreneauEntity buildCreneau() {
        ConfigCreneauEntity entity = new ConfigCreneauEntity();
        entity.setId(creneauId);
        entity.setOrganisationId(organisationId);
        entity.setTypeServiceId(typeServiceId);
        entity.setJourSemaine(2);
        entity.setHeureDebut(LocalTime.of(9, 0));
        entity.setHeureFin(LocalTime.of(12, 0));
        entity.setDureeMinutes(30);
        entity.setQuotaMax(8);
        entity.setActif(true);
        return entity;
    }
}
