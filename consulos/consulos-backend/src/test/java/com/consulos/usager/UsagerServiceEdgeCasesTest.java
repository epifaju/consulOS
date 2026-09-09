package com.consulos.usager;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import com.consulos.usager.dto.DoublonGroupDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.dto.UsagerSummaryDTO;
import com.consulos.usager.dto.UsagerUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Écarts non couverts par UsagerServiceTest : update, search, searchQuick, findDoublons.
 */
@ExtendWith(MockitoExtension.class)
class UsagerServiceEdgeCasesTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private UsagerMapper usagerMapper;

    @InjectMocks
    private UsagerService usagerService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID usagerId;
    private UsagerEntity usager;
    private UsagerDetailDTO detailDTO;
    private UsagerSummaryDTO summaryDTO;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();

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

        summaryDTO = new UsagerSummaryDTO(
                usagerId,
                "GNB-2026-00001",
                "Camara",
                "Mamadou",
                "GNB",
                TypeUsager.DIASPORA,
                null,
                "m@test.local",
                true
        );
    }

    @Test
    void update_shouldApplyMapperAndReturnDetail() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        doAnswer(invocation -> {
            UsagerEntity entity = invocation.getArgument(0);
            entity.setEmail("nouveau@test.local");
            return null;
        }).when(usagerMapper).updateEntity(eq(usager), any(UsagerUpdateDTO.class));
        when(usagerRepository.save(usager)).thenReturn(usager);
        when(usagerMapper.toDetail(usager)).thenReturn(detailDTO);

        UsagerDetailDTO result = usagerService.update(
                usagerId,
                new UsagerUpdateDTO(null, null, null, null, null, null, null, "nouveau@test.local", null, null, null),
                organisationId
        );

        assertThat(usager.getEmail()).isEqualTo("nouveau@test.local");
        assertThat(result).isEqualTo(detailDTO);
        verify(usagerMapper).updateEntity(eq(usager), any(UsagerUpdateDTO.class));
    }

    @Test
    void update_whenWrongOrganisation_shouldThrowNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> usagerService.update(
                usagerId,
                new UsagerUpdateDTO("X", null, null, null, null, null, null, null, null, null, null),
                otherOrganisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(usagerRepository, never()).save(any());
    }

    @Test
    void search_withoutQuery_shouldPageActiveUsagers() {
        when(usagerRepository.findByOrganisationIdAndActifTrue(eq(organisationId), any()))
                .thenReturn(new PageImpl<>(List.of(usager), PageRequest.of(0, 20), 1));
        when(usagerMapper.toSummary(usager)).thenReturn(summaryDTO);

        var page = usagerService.search("  ", organisationId, PageRequest.of(0, 20));

        assertThat(page.content()).containsExactly(summaryDTO);
        assertThat(page.totalElements()).isEqualTo(1);
        verify(usagerRepository, never()).searchFullText(any(), any());
    }

    @Test
    void search_withQuery_shouldUseFullText() {
        when(usagerRepository.searchFullText(organisationId, "Camara")).thenReturn(List.of(usager));
        when(usagerMapper.toSummary(usager)).thenReturn(summaryDTO);

        var page = usagerService.search(" Camara ", organisationId, PageRequest.of(0, 20));

        assertThat(page.content()).containsExactly(summaryDTO);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void searchQuick_withoutQuery_shouldReturnEmpty() {
        assertThat(usagerService.searchQuick(" ", organisationId)).isEmpty();
        verify(usagerRepository, never()).searchFullText(any(), any());
    }

    @Test
    void searchQuick_withQuery_shouldReturnSummaries() {
        when(usagerRepository.searchFullText(organisationId, "Mam")).thenReturn(List.of(usager));
        when(usagerMapper.toSummary(usager)).thenReturn(summaryDTO);

        assertThat(usagerService.searchQuick("Mam", organisationId)).containsExactly(summaryDTO);
    }

    @Test
    void findDoublons_whenIncompleteCriteria_shouldReturnEmpty() {
        assertThat(usagerService.findDoublons(" ", LocalDate.of(1990, 1, 1), "GNB", organisationId, null))
                .isEmpty();
        assertThat(usagerService.findDoublons("Camara", null, "GNB", organisationId, null)).isEmpty();
        verify(usagerRepository, never()).findPotentialDoublons(any(), any(), any(), any(), any());
    }

    @Test
    void findDoublons_whenMatches_shouldReturnGroup() {
        LocalDate birth = LocalDate.of(1990, 1, 1);
        when(usagerRepository.findPotentialDoublons(organisationId, "Camara", birth, "GNB", usagerId))
                .thenReturn(List.of(usager));
        when(usagerMapper.toSummary(usager)).thenReturn(summaryDTO);

        List<DoublonGroupDTO> groups = usagerService.findDoublons(
                "Camara", birth, "GNB", organisationId, usagerId
        );

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().similaires()).containsExactly(summaryDTO);
        assertThat(groups.getFirst().motif()).contains("même nom");
    }
}
