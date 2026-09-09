package com.consulos.usager;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import com.consulos.usager.dto.DossierSummaryDTO;
import com.consulos.usager.dto.DoublonGroupDTO;
import com.consulos.usager.dto.UsagerCreateDTO;
import com.consulos.usager.dto.UsagerCreateResponseDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
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
class UsagerServiceRemainingGapsTest {

    private static final UUID NO_EXCLUDE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private UsagerMapper usagerMapper;

    @InjectMocks
    private UsagerService usagerService;

    private UUID organisationId;
    private UUID usagerId;
    private OrganisationEntity organisation;
    private UsagerEntity usager;
    private UsagerDetailDTO detailDTO;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();

        organisation = new OrganisationEntity();
        organisation.setId(organisationId);
        organisation.setCode("GNB");

        usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setNom("Camara");
        usager.setPrenom("Mamadou");
        usager.setActif(true);
        usager.setNumero("GNB-2026-00001");

        detailDTO = new UsagerDetailDTO(
                usagerId,
                "GNB-2026-00001",
                "Camara",
                "Mamadou",
                LocalDate.of(1990, 1, 1),
                null,
                "GNB",
                TypeUsager.DIASPORA,
                null,
                "m@test.local",
                null,
                null,
                null,
                Map.of(),
                true,
                organisationId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void findById_whenInactive_shouldThrowNotFound() {
        usager.setActif(false);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> usagerService.findById(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void findById_whenMissing_shouldThrowNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usagerService.findById(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));
    }

    @Test
    void create_whenOrganisationMissing_shouldThrowOrgNotFound() {
        UsagerCreateDTO dto = new UsagerCreateDTO(
                "Camara", "Mamadou", null, null, null,
                TypeUsager.DIASPORA, null, null, null, null, null
        );
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usagerService.create(dto, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("ORG_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(usagerRepository, never()).save(any());
    }

    @Test
    void create_whenTypeUsagerNullAfterMapping_shouldDefaultToDiaspora() {
        UsagerCreateDTO dto = new UsagerCreateDTO(
                "Camara", "Mamadou", null, null, null,
                null, null, null, null, null, null
        );
        usager.setTypeUsager(null);

        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(usagerMapper.toEntity(dto)).thenReturn(usager);
        when(usagerRepository.nextUsagerSequence()).thenReturn(1L);
        when(usagerRepository.save(any(UsagerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerMapper.toDetail(any(UsagerEntity.class))).thenReturn(detailDTO);

        UsagerCreateResponseDTO result = usagerService.create(dto, organisationId);

        assertThat(usager.getTypeUsager()).isEqualTo(TypeUsager.DIASPORA);
        assertThat(result.usager()).isEqualTo(detailDTO);
    }

    @Test
    void create_whenOrgCodeWithoutHyphen_shouldUseFullCodeAsPrefix() {
        UsagerCreateDTO dto = new UsagerCreateDTO(
                "Camara", "Mamadou", null, null, null,
                TypeUsager.DIASPORA, null, null, null, null, null
        );

        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(usagerMapper.toEntity(dto)).thenReturn(usager);
        when(usagerRepository.nextUsagerSequence()).thenReturn(7L);
        when(usagerRepository.save(any(UsagerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerMapper.toDetail(any(UsagerEntity.class))).thenReturn(detailDTO);

        usagerService.create(dto, organisationId);

        ArgumentCaptor<UsagerEntity> captor = ArgumentCaptor.forClass(UsagerEntity.class);
        verify(usagerRepository).save(captor.capture());
        assertThat(captor.getValue().getNumero())
                .isEqualTo("GNB-" + Year.now().getValue() + "-00007");
    }

    @Test
    void findDoublons_whenBlankNomOrNullDate_shouldReturnEmpty() {
        assertThat(usagerService.findDoublons(" ", LocalDate.of(1990, 1, 1), "GNB", organisationId, null))
                .isEmpty();
        assertThat(usagerService.findDoublons("Camara", null, "GNB", organisationId, null)).isEmpty();
        verify(usagerRepository, never()).findPotentialDoublons(any(), any(), any(), any(), any());
    }

    @Test
    void findDoublons_whenNoSimilaires_shouldReturnEmpty() {
        LocalDate birth = LocalDate.of(1990, 1, 1);
        when(usagerRepository.findPotentialDoublons(organisationId, "Camara", birth, "GNB", usagerId))
                .thenReturn(List.of());

        List<DoublonGroupDTO> groups = usagerService.findDoublons(
                "Camara", birth, "GNB", organisationId, usagerId
        );

        assertThat(groups).isEmpty();
    }

    @Test
    void findDoublons_whenExcludeIdNull_shouldUseZeroUuid() {
        LocalDate birth = LocalDate.of(1990, 1, 1);
        when(usagerRepository.findPotentialDoublons(
                organisationId, "Camara", birth, "GNB", NO_EXCLUDE_ID
        )).thenReturn(List.of());

        assertThat(usagerService.findDoublons("Camara", birth, "GNB", organisationId, null)).isEmpty();

        verify(usagerRepository).findPotentialDoublons(
                organisationId, "Camara", birth, "GNB", NO_EXCLUDE_ID
        );
    }

    @Test
    void getHistorique_whenActive_shouldReturnEmptyList() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        List<DossierSummaryDTO> historique = usagerService.getHistorique(usagerId, organisationId);

        assertThat(historique).isEmpty();
    }

    @Test
    void getHistorique_whenInactive_shouldThrowNotFound() {
        usager.setActif(false);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> usagerService.getHistorique(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));
    }
}
