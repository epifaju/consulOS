package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.portal.dto.PortalDossierSummaryDTO;
import com.consulos.portal.dto.PortalSessionResponseDTO;
import com.consulos.usager.UsagerEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAccessService {

    public static final String TYPE_MAGIC_LINK = "MAGIC_LINK";
    public static final String TYPE_SESSION = "SESSION";

    private static final int MAGIC_LINK_HOURS = 1;
    private static final int SESSION_HOURS = 8;

    private final PortalDossierLookupRepository dossierLookupRepository;
    private final PortalUsagerLookupRepository usagerLookupRepository;
    private final PortalAccessTokenRepository tokenRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final PortalMailService portalMailService;

    @Transactional
    public void requestAccess(String reference, String email) {
        if (!StringUtils.hasText(reference) || !StringUtils.hasText(email)) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        dossierLookupRepository.findActiveByReference(reference.trim()).ifPresent(dossier -> {
            usagerLookupRepository
                    .findByIdAndOrganisationIdAndActifTrue(dossier.getUsagerId(), dossier.getOrganisationId())
                    .filter(u -> StringUtils.hasText(u.getEmail())
                            && normalizedEmail.equals(u.getEmail().trim().toLowerCase(Locale.ROOT)))
                    .ifPresent(usager -> {
                        String rawToken = generateRawToken();
                        persistToken(dossier, usager.getId(), rawToken, TYPE_MAGIC_LINK, MAGIC_LINK_HOURS);
                        portalMailService.sendMagicLink(usager.getEmail(), rawToken, dossier.getReference());
                    });
        });
    }

    @Transactional
    public PortalSessionResponseDTO openSession(String magicToken) {
        PortalAccessTokenEntity magic = tokenRepository
                .findValidToken(hash(magicToken), TYPE_MAGIC_LINK, OffsetDateTime.now(ZoneOffset.UTC))
                .orElseThrow(() -> new BusinessException(
                        "PORTAL_TOKEN_INVALID", "Lien d'accès invalide ou expiré", HttpStatus.UNAUTHORIZED));

        magic.setConsumedAt(OffsetDateTime.now(ZoneOffset.UTC));
        tokenRepository.save(magic);

        DossierEntity dossier = dossierLookupRepository.findById(magic.getDossierId())
                .filter(d -> Boolean.TRUE.equals(d.getActif()))
                .orElseThrow(() -> new BusinessException(
                        "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));

        UsagerEntity usager = usagerLookupRepository
                .findByIdAndOrganisationIdAndActifTrue(magic.getUsagerId(), magic.getOrganisationId())
                .orElseThrow(() -> new BusinessException(
                        "USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));

        String sessionToken = generateRawToken();
        persistToken(dossier, usager.getId(), sessionToken, TYPE_SESSION, SESSION_HOURS);

        return new PortalSessionResponseDTO(sessionToken, toSummary(dossier, usager));
    }

    @Transactional(readOnly = true)
    public PortalSession resolveSession(String sessionToken) {
        PortalAccessTokenEntity token = tokenRepository
                .findValidToken(hash(sessionToken), TYPE_SESSION, OffsetDateTime.now(ZoneOffset.UTC))
                .orElseThrow(() -> new BusinessException(
                        "PORTAL_SESSION_INVALID", "Session portail invalide ou expirée", HttpStatus.UNAUTHORIZED));

        DossierEntity dossier = dossierLookupRepository.findById(token.getDossierId())
                .filter(d -> Boolean.TRUE.equals(d.getActif()))
                .orElseThrow(() -> new BusinessException(
                        "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));

        return new PortalSession(
                token.getOrganisationId(),
                token.getDossierId(),
                token.getUsagerId(),
                dossier.getReference()
        );
    }

    @Transactional(readOnly = true)
    public PortalDossierSummaryDTO getDossierSummary(PortalSession session) {
        DossierEntity dossier = dossierLookupRepository.findById(session.dossierId())
                .filter(d -> Boolean.TRUE.equals(d.getActif())
                        && session.organisationId().equals(d.getOrganisationId()))
                .orElseThrow(() -> new BusinessException(
                        "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
        UsagerEntity usager = usagerLookupRepository
                .findByIdAndOrganisationIdAndActifTrue(session.usagerId(), session.organisationId())
                .orElseThrow(() -> new BusinessException(
                        "USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        return toSummary(dossier, usager);
    }

    private void persistToken(
            DossierEntity dossier,
            UUID usagerId,
            String rawToken,
            String type,
            int hoursValid
    ) {
        PortalAccessTokenEntity entity = new PortalAccessTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setOrganisationId(dossier.getOrganisationId());
        entity.setDossierId(dossier.getId());
        entity.setUsagerId(usagerId);
        entity.setTokenHash(hash(rawToken));
        entity.setTokenType(type);
        entity.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(hoursValid));
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        tokenRepository.save(entity);
    }

    private PortalDossierSummaryDTO toSummary(DossierEntity dossier, UsagerEntity usager) {
        String typeLibelle = typeServiceRepository
                .findByIdAndOrganisationIdAndActifTrue(dossier.getTypeServiceId(), dossier.getOrganisationId())
                .map(TypeServiceEntity::getLibelle)
                .orElse("Service consulaire");
        return new PortalDossierSummaryDTO(
                dossier.getId(),
                dossier.getReference(),
                dossier.getStatut() != null ? dossier.getStatut().name() : null,
                typeLibelle,
                dossier.getDateSoumission(),
                dossier.getDateEcheance(),
                usager.getPrenom(),
                usager.getNom()
        );
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        try {
            SecureRandom.getInstanceStrong().nextBytes(bytes);
        } catch (Exception ex) {
            new SecureRandom().nextBytes(bytes);
        }
        return HexFormat.of().formatHex(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
