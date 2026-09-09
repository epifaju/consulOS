package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.portal.dto.PortalRdvCreateDTO;
import com.consulos.rendezvous.DisponibiliteService;
import com.consulos.rendezvous.RdvService;
import com.consulos.rendezvous.dto.RdvCreateDTO;
import com.consulos.rendezvous.dto.RdvDetailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class PortalRdvServiceTenantTest {

    @Mock
    private DisponibiliteService disponibiliteService;
    @Mock
    private RdvService rdvService;
    @Mock
    private PortalDossierLookupRepository dossierLookupRepository;

    @InjectMocks
    private PortalRdvService portalRdvService;

    private PortalSession session;
    private UUID typeServiceId;
    private DossierEntity dossier;

    @BeforeEach
    void setUp() {
        typeServiceId = UUID.randomUUID();
        session = new PortalSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DOS-2026-0042"
        );
        dossier = new DossierEntity();
        dossier.setId(session.dossierId());
        dossier.setOrganisationId(session.organisationId());
        dossier.setTypeServiceId(typeServiceId);
        dossier.setActif(true);
    }

    @Test
    void disponibilites_whenDossierOtherOrg_shouldThrowNotFound() {
        dossier.setOrganisationId(UUID.randomUUID());
        when(dossierLookupRepository.findById(session.dossierId())).thenReturn(Optional.of(dossier));

        assertThatThrownBy(() -> portalRdvService.disponibilites(session, LocalDate.of(2026, 9, 15)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(disponibiliteService, never()).getDisponibilites(any(), any(), any());
    }

    @Test
    void disponibilites_whenDossierInactive_shouldThrowNotFound() {
        dossier.setActif(false);
        when(dossierLookupRepository.findById(session.dossierId())).thenReturn(Optional.of(dossier));

        assertThatThrownBy(() -> portalRdvService.disponibilites(session, LocalDate.of(2026, 9, 15)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(disponibiliteService, never()).getDisponibilites(any(), any(), any());
    }

    @Test
    void create_whenDossierInactive_shouldThrowNotFound() {
        dossier.setActif(false);
        when(dossierLookupRepository.findById(session.dossierId())).thenReturn(Optional.of(dossier));

        assertThatThrownBy(() -> portalRdvService.create(
                session, new PortalRdvCreateDTO(OffsetDateTime.parse("2026-09-16T10:00:00Z"), 30, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(rdvService, never()).create(any(), any());
    }

    @Test
    void create_whenDossierMissing_shouldThrowNotFound() {
        when(dossierLookupRepository.findById(session.dossierId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalRdvService.create(
                session, new PortalRdvCreateDTO(OffsetDateTime.parse("2026-09-16T10:00:00Z"), 30, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(rdvService, never()).create(any(), any());
    }

    @Test
    void create_whenDureeProvided_shouldUseDtoDuree() {
        when(dossierLookupRepository.findById(session.dossierId())).thenReturn(Optional.of(dossier));
        OffsetDateTime dateHeure = OffsetDateTime.parse("2026-09-16T10:00:00Z");
        RdvDetailDTO detail = org.mockito.Mockito.mock(RdvDetailDTO.class);
        when(rdvService.create(any(), eq(session.organisationId()))).thenReturn(detail);

        RdvDetailDTO result = portalRdvService.create(
                session, new PortalRdvCreateDTO(dateHeure, 45, "notes"));

        assertThat(result).isSameAs(detail);
        ArgumentCaptor<RdvCreateDTO> captor = ArgumentCaptor.forClass(RdvCreateDTO.class);
        verify(rdvService).create(captor.capture(), eq(session.organisationId()));
        assertThat(captor.getValue().dureeMinutes()).isEqualTo(45);
        assertThat(captor.getValue().notes()).isEqualTo("notes");
    }
}
