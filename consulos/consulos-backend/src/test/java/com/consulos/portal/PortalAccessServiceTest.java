package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierStatut;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.portal.dto.PortalDossierSummaryDTO;
import com.consulos.portal.dto.PortalSessionResponseDTO;
import com.consulos.usager.UsagerEntity;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalAccessServiceTest {

    @Mock
    private PortalDossierLookupRepository dossierLookupRepository;
    @Mock
    private PortalUsagerLookupRepository usagerLookupRepository;
    @Mock
    private PortalAccessTokenRepository tokenRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private PortalMailService portalMailService;

    @InjectMocks
    private PortalAccessService portalAccessService;

    private UUID organisationId;
    private UUID dossierId;
    private UUID usagerId;
    private UUID typeServiceId;
    private DossierEntity dossier;
    private UsagerEntity usager;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();

        dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setReference("DOS-2026-0001");
        dossier.setTypeServiceId(typeServiceId);
        dossier.setStatut(DossierStatut.SOUMIS);
        dossier.setActif(true);

        usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setEmail("diaspora@example.com");
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        usager.setActif(true);
    }

    @Test
    void requestAccess_whenEmailMatches_shouldPersistMagicLinkAndSendMail() {
        when(dossierLookupRepository.findActiveByReference("DOS-2026-0001")).thenReturn(Optional.of(dossier));
        when(usagerLookupRepository.findByIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(Optional.of(usager));
        when(tokenRepository.save(any(PortalAccessTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        portalAccessService.requestAccess(" DOS-2026-0001 ", "  Diaspora@Example.com ");

        ArgumentCaptor<PortalAccessTokenEntity> tokenCaptor = ArgumentCaptor.forClass(PortalAccessTokenEntity.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PortalAccessTokenEntity saved = tokenCaptor.getValue();
        assertThat(saved.getTokenType()).isEqualTo(PortalAccessService.TYPE_MAGIC_LINK);
        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getDossierId()).isEqualTo(dossierId);
        assertThat(saved.getUsagerId()).isEqualTo(usagerId);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getConsumedAt()).isNull();

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(portalMailService).sendMagicLink(eq("diaspora@example.com"), rawTokenCaptor.capture(), eq("DOS-2026-0001"));
        assertThat(PortalAccessService.hash(rawTokenCaptor.getValue())).isEqualTo(saved.getTokenHash());
    }

    @Test
    void requestAccess_whenEmailMismatch_shouldNotLeakOrSendMail() {
        when(dossierLookupRepository.findActiveByReference("DOS-2026-0001")).thenReturn(Optional.of(dossier));
        when(usagerLookupRepository.findByIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(Optional.of(usager));

        portalAccessService.requestAccess("DOS-2026-0001", "autre@example.com");

        verify(tokenRepository, never()).save(any());
        verify(portalMailService, never()).sendMagicLink(any(), any(), any());
    }

    @Test
    void requestAccess_whenBlankInput_shouldNoOp() {
        portalAccessService.requestAccess(" ", "email@example.com");
        portalAccessService.requestAccess("DOS-1", "  ");

        verify(dossierLookupRepository, never()).findActiveByReference(any());
    }

    @Test
    void openSession_shouldConsumeMagicLinkAndCreateSessionToken() {
        String magicRaw = "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899";
        PortalAccessTokenEntity magic = new PortalAccessTokenEntity();
        magic.setId(UUID.randomUUID());
        magic.setOrganisationId(organisationId);
        magic.setDossierId(dossierId);
        magic.setUsagerId(usagerId);
        magic.setTokenHash(PortalAccessService.hash(magicRaw));
        magic.setTokenType(PortalAccessService.TYPE_MAGIC_LINK);

        when(tokenRepository.findValidToken(
                eq(PortalAccessService.hash(magicRaw)),
                eq(PortalAccessService.TYPE_MAGIC_LINK),
                any()
        )).thenReturn(Optional.of(magic));
        when(dossierLookupRepository.findById(dossierId)).thenReturn(Optional.of(dossier));
        when(usagerLookupRepository.findByIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(Optional.of(usager));
        when(tokenRepository.save(any(PortalAccessTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService("Passeport")));

        PortalSessionResponseDTO response = portalAccessService.openSession(magicRaw);

        assertThat(magic.getConsumedAt()).isNotNull();
        assertThat(response.sessionToken()).isNotBlank();
        assertThat(response.dossier().reference()).isEqualTo("DOS-2026-0001");
        assertThat(response.dossier().typeServiceLibelle()).isEqualTo("Passeport");

        ArgumentCaptor<PortalAccessTokenEntity> captor = ArgumentCaptor.forClass(PortalAccessTokenEntity.class);
        verify(tokenRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getTokenType()).isEqualTo(PortalAccessService.TYPE_SESSION);
        assertThat(PortalAccessService.hash(response.sessionToken()))
                .isEqualTo(captor.getAllValues().get(1).getTokenHash());
    }

    @Test
    void openSession_whenMagicInvalid_shouldThrowUnauthorized() {
        when(tokenRepository.findValidToken(any(), eq(PortalAccessService.TYPE_MAGIC_LINK), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalAccessService.openSession("bad-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("PORTAL_TOKEN_INVALID");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void resolveSession_shouldReturnPortalSession() {
        String sessionRaw = "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff";
        PortalAccessTokenEntity sessionToken = new PortalAccessTokenEntity();
        sessionToken.setOrganisationId(organisationId);
        sessionToken.setDossierId(dossierId);
        sessionToken.setUsagerId(usagerId);
        sessionToken.setTokenType(PortalAccessService.TYPE_SESSION);

        when(tokenRepository.findValidToken(
                eq(PortalAccessService.hash(sessionRaw)),
                eq(PortalAccessService.TYPE_SESSION),
                any()
        )).thenReturn(Optional.of(sessionToken));
        when(dossierLookupRepository.findById(dossierId)).thenReturn(Optional.of(dossier));

        PortalSession session = portalAccessService.resolveSession(sessionRaw);

        assertThat(session.organisationId()).isEqualTo(organisationId);
        assertThat(session.dossierId()).isEqualTo(dossierId);
        assertThat(session.usagerId()).isEqualTo(usagerId);
        assertThat(session.dossierReference()).isEqualTo("DOS-2026-0001");
    }

    @Test
    void resolveSession_whenInvalid_shouldThrowUnauthorized() {
        when(tokenRepository.findValidToken(any(), eq(PortalAccessService.TYPE_SESSION), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalAccessService.resolveSession("expired"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("PORTAL_SESSION_INVALID"));
    }

    @Test
    void getDossierSummary_shouldMapUsagerAndType() {
        PortalSession session = new PortalSession(organisationId, dossierId, usagerId, "DOS-2026-0001");
        when(dossierLookupRepository.findById(dossierId)).thenReturn(Optional.of(dossier));
        when(usagerLookupRepository.findByIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(Optional.of(usager));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.empty());

        PortalDossierSummaryDTO summary = portalAccessService.getDossierSummary(session);

        assertThat(summary.usagerPrenom()).isEqualTo("Aminata");
        assertThat(summary.usagerNom()).isEqualTo("Camara");
        assertThat(summary.typeServiceLibelle()).isEqualTo("Service consulaire");
        assertThat(summary.statut()).isEqualTo("SOUMIS");
    }

    private TypeServiceEntity typeService(String libelle) {
        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setLibelle(libelle);
        return type;
    }
}
