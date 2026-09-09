package com.consulos.rendezvous;

import com.consulos.rendezvous.dto.JourFermetureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametresServiceFermetureUpdateTest {

    @Mock
    private ConfigCreneauRepository configCreneauRepository;
    @Mock
    private JourFermetureRepository jourFermetureRepository;

    @InjectMocks
    private ParametresService parametresService;

    private UUID organisationId;
    private UUID fermetureId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        fermetureId = UUID.randomUUID();
    }

    @Test
    void updateJourFermeture_shouldPersistChanges() {
        JourFermetureEntity entity = new JourFermetureEntity();
        entity.setId(fermetureId);
        entity.setOrganisationId(organisationId);
        entity.setDateFermeture(LocalDate.of(2026, 5, 1));
        entity.setMotif("Ancien");

        when(jourFermetureRepository.findByIdAndOrganisationId(fermetureId, organisationId))
                .thenReturn(Optional.of(entity));
        when(jourFermetureRepository.save(any(JourFermetureEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JourFermetureDTO result = parametresService.updateJourFermeture(
                fermetureId,
                new JourFermetureDTO(fermetureId, LocalDate.of(2026, 5, 8), "Ascension"),
                organisationId
        );

        assertThat(entity.getDateFermeture()).isEqualTo(LocalDate.of(2026, 5, 8));
        assertThat(entity.getMotif()).isEqualTo("Ascension");
        assertThat(result.motif()).isEqualTo("Ascension");
        assertThat(result.dateFermeture()).isEqualTo(LocalDate.of(2026, 5, 8));
        verify(jourFermetureRepository).save(entity);
    }
}
