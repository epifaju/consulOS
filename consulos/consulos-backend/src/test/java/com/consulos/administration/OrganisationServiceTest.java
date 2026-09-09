package com.consulos.administration;

import com.consulos.administration.dto.OrganisationDTO;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private OrganisationService organisationService;

    private UUID organisationId;
    private OrganisationEntity entity;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        entity = new OrganisationEntity();
        entity.setId(organisationId);
        entity.setCode("PAR");
        entity.setNom("Consulat de Paris");
        entity.setPaysCode("FRA");
        entity.setLangueDefaut("fr");
        entity.setDeviseDefaut("EUR");
        entity.setActif(true);
    }

    @Test
    void get_happy() {
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(entity));

        OrganisationDTO result = organisationService.get(organisationId);

        assertThat(result).isEqualTo(new OrganisationDTO(
                organisationId, "PAR", "Consulat de Paris", "FRA", "fr", "EUR", true));
    }

    @Test
    void get_orgNotFound() {
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organisationService.get(organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("ORG_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void update_nomPaysCodeAndOptionalFields() {
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(entity));
        when(organisationRepository.save(any(OrganisationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganisationDTO dto = new OrganisationDTO(
                organisationId,
                "PAR",
                "Consulat général de Paris",
                "FRA",
                "en",
                "USD",
                false
        );

        OrganisationDTO result = organisationService.update(organisationId, dto);

        ArgumentCaptor<OrganisationEntity> captor = ArgumentCaptor.forClass(OrganisationEntity.class);
        verify(organisationRepository).save(captor.capture());
        OrganisationEntity saved = captor.getValue();
        assertThat(saved.getNom()).isEqualTo("Consulat général de Paris");
        assertThat(saved.getPaysCode()).isEqualTo("FRA");
        assertThat(saved.getLangueDefaut()).isEqualTo("en");
        assertThat(saved.getDeviseDefaut()).isEqualTo("USD");
        assertThat(saved.getActif()).isFalse();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(result.nom()).isEqualTo("Consulat général de Paris");
        assertThat(result.langueDefaut()).isEqualTo("en");
        assertThat(result.deviseDefaut()).isEqualTo("USD");
        assertThat(result.actif()).isFalse();
    }
}
