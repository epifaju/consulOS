package com.consulos.usager;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import com.consulos.usager.dto.DoublonGroupDTO;
import com.consulos.usager.dto.UsagerCreateDTO;
import com.consulos.usager.dto.UsagerCreateResponseDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.dto.UsagerSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsagerServiceTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private UsagerMapper usagerMapper;

    @InjectMocks
    private UsagerService usagerService;

    private UUID organisationId;
    private OrganisationEntity organisation;
    private UsagerEntity usagerEntity;
    private UsagerDetailDTO detailDTO;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        organisation = new OrganisationEntity();
        organisation.setId(organisationId);
        organisation.setCode("GNB-FR");

        usagerEntity = new UsagerEntity();
        usagerEntity.setId(UUID.randomUUID());
        usagerEntity.setOrganisationId(organisationId);
        usagerEntity.setNom("Camara");
        usagerEntity.setPrenom("Mamadou");
        usagerEntity.setActif(true);

        detailDTO = new UsagerDetailDTO(
                usagerEntity.getId(),
                "GNB-2026-00001",
                "Camara",
                "Mamadou",
                LocalDate.of(1990, 1, 1),
                null,
                "GNB",
                TypeUsager.DIASPORA,
                "+33612345678",
                "test@email.com",
                null,
                null,
                null,
                java.util.Map.of(),
                true,
                organisationId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void create_shouldGenerateNumeroAndReturnDetail() {
        UsagerCreateDTO dto = new UsagerCreateDTO(
                "Camara", "Mamadou", null, null, "GNB",
                TypeUsager.DIASPORA, "+33612345678", "test@email.com",
                null, null, null
        );

        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(usagerMapper.toEntity(dto)).thenReturn(usagerEntity);
        when(usagerRepository.nextUsagerSequence()).thenReturn(1L);
        when(usagerRepository.save(any(UsagerEntity.class))).thenAnswer(invocation -> {
            UsagerEntity saved = invocation.getArgument(0);
            saved.setNumero("GNB-" + java.time.Year.now().getValue() + "-00001");
            return saved;
        });
        when(usagerMapper.toDetail(any(UsagerEntity.class))).thenReturn(detailDTO);

        UsagerCreateResponseDTO result = usagerService.create(dto, organisationId);

        assertThat(result.usager()).isNotNull();
        assertThat(result.usager().numero()).matches("GNB-\\d{4}-\\d{5}");
        verify(usagerRepository).save(any(UsagerEntity.class));
    }

    @Test
    void create_withDoublon_shouldIncludeWarning() {
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        UsagerCreateDTO dto = new UsagerCreateDTO(
                "Camara", "Mamadou", birthDate, null, "GNB",
                TypeUsager.DIASPORA, null, null, null, null, null
        );
        UsagerEntity duplicate = new UsagerEntity();
        duplicate.setId(UUID.randomUUID());
        duplicate.setNom("Camara");
        duplicate.setPrenom("Mamadou");
        duplicate.setActif(true);

        when(usagerRepository.findPotentialDoublons(eq(organisationId), eq("Camara"), eq(birthDate), eq("GNB"), any()))
                .thenReturn(List.of(duplicate));
        when(usagerMapper.toSummary(duplicate)).thenReturn(new UsagerSummaryDTO(
                duplicate.getId(), "GNB-2024-00001", "Camara", "Mamadou",
                "GNB", TypeUsager.DIASPORA, null, null, true
        ));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(usagerMapper.toEntity(dto)).thenReturn(usagerEntity);
        when(usagerRepository.nextUsagerSequence()).thenReturn(2L);
        when(usagerRepository.save(any(UsagerEntity.class))).thenReturn(usagerEntity);
        when(usagerMapper.toDetail(usagerEntity)).thenReturn(detailDTO);

        UsagerCreateResponseDTO result = usagerService.create(dto, organisationId);

        assertThat(result.doublonsPotentiels()).hasSize(1);
        assertThat(result.doublonsPotentiels().getFirst().similaires()).hasSize(1);
    }

    @Test
    void findById_withWrongOrganisation_shouldThrowNotFound() {
        UUID otherOrg = UUID.randomUUID();
        usagerEntity.setOrganisationId(otherOrg);
        when(usagerRepository.findById(usagerEntity.getId())).thenReturn(Optional.of(usagerEntity));

        assertThatThrownBy(() -> usagerService.findById(usagerEntity.getId(), organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));
    }

    @Test
    void softDelete_shouldSetActifFalse() {
        when(usagerRepository.findById(usagerEntity.getId())).thenReturn(Optional.of(usagerEntity));
        when(usagerRepository.save(usagerEntity)).thenReturn(usagerEntity);

        usagerService.softDelete(usagerEntity.getId(), organisationId);

        assertThat(usagerEntity.getActif()).isFalse();
        verify(usagerRepository).save(usagerEntity);
    }
}
