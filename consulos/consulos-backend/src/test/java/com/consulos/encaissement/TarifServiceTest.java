package com.consulos.encaissement;

import com.consulos.common.BusinessException;
import com.consulos.encaissement.dto.TarifDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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
class TarifServiceTest {

    @Mock
    private TarifRepository tarifRepository;

    @InjectMocks
    private TarifService tarifService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID tarifId;
    private UUID typeServiceId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        tarifId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
    }

    @Test
    void list_shouldReturnActiveTarifsForOrganisation() {
        TarifEntity entity = buildTarif();
        when(tarifRepository.findByOrganisationIdAndActifTrue(organisationId)).thenReturn(List.of(entity));

        List<TarifDTO> result = tarifService.list(organisationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().libelle()).isEqualTo("Passeport");
        assertThat(result.getFirst().montant()).isEqualByComparingTo("85.00");
        assertThat(result.getFirst().devise()).isEqualTo("EUR");
    }

    @Test
    void create_shouldPersistTarifScopedToOrganisation() {
        TarifDTO dto = new TarifDTO(null, typeServiceId, "Visa court séjour", new BigDecimal("60.00"), null, null);
        when(tarifRepository.save(any(TarifEntity.class))).thenAnswer(invocation -> {
            TarifEntity saved = invocation.getArgument(0);
            saved.setId(tarifId);
            return saved;
        });

        TarifDTO result = tarifService.create(dto, organisationId);

        ArgumentCaptor<TarifEntity> captor = ArgumentCaptor.forClass(TarifEntity.class);
        verify(tarifRepository).save(captor.capture());
        TarifEntity saved = captor.getValue();
        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getTypeServiceId()).isEqualTo(typeServiceId);
        assertThat(saved.getLibelle()).isEqualTo("Visa court séjour");
        assertThat(saved.getMontant()).isEqualByComparingTo("60.00");
        assertThat(saved.getDevise()).isEqualTo("EUR");
        assertThat(result.id()).isEqualTo(tarifId);
    }

    @Test
    void create_withExplicitDeviseAndActif_shouldApplyValues() {
        TarifDTO dto = new TarifDTO(null, typeServiceId, "Légalisation", new BigDecimal("25.00"), "XOF", false);
        when(tarifRepository.save(any(TarifEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tarifService.create(dto, organisationId);

        ArgumentCaptor<TarifEntity> captor = ArgumentCaptor.forClass(TarifEntity.class);
        verify(tarifRepository).save(captor.capture());
        assertThat(captor.getValue().getDevise()).isEqualTo("XOF");
        assertThat(captor.getValue().getActif()).isFalse();
    }

    @Test
    void update_shouldModifyExistingTarif() {
        TarifEntity entity = buildTarif();
        when(tarifRepository.findByIdAndOrganisationId(tarifId, organisationId)).thenReturn(Optional.of(entity));
        when(tarifRepository.save(any(TarifEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TarifDTO dto = new TarifDTO(tarifId, typeServiceId, "Passeport express", new BigDecimal("120.00"), "EUR", true);
        TarifDTO result = tarifService.update(tarifId, dto, organisationId);

        assertThat(entity.getLibelle()).isEqualTo("Passeport express");
        assertThat(entity.getMontant()).isEqualByComparingTo("120.00");
        assertThat(result.libelle()).isEqualTo("Passeport express");
    }

    @Test
    void update_whenOtherOrganisation_shouldThrowNotFound() {
        when(tarifRepository.findByIdAndOrganisationId(tarifId, otherOrganisationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarifService.update(
                tarifId,
                new TarifDTO(tarifId, typeServiceId, "X", BigDecimal.ONE, "EUR", true),
                otherOrganisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("TARIF_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(tarifRepository, never()).save(any());
    }

    private TarifEntity buildTarif() {
        TarifEntity entity = new TarifEntity();
        entity.setId(tarifId);
        entity.setOrganisationId(organisationId);
        entity.setTypeServiceId(typeServiceId);
        entity.setLibelle("Passeport");
        entity.setMontant(new BigDecimal("85.00"));
        entity.setDevise("EUR");
        entity.setActif(true);
        return entity;
    }
}
