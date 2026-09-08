package com.consulos.reseau;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.common.BusinessException;
import com.consulos.reseau.dto.ReseauAttachPosteRequestDTO;
import com.consulos.reseau.dto.ReseauMembreDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class ReseauAdminService {

    public static final String ROLE_ADMIN = "ADMIN";

    private final ReseauRepository reseauRepository;
    private final ReseauMembreRepository membreRepository;
    private final ReseauMembreAdminRepository membreAdminRepository;
    private final ReseauPosteAdminRepository posteAdminRepository;
    private final ReseauOrganisationLookupRepository organisationLookupRepository;
    private final ReseauUserLookupRepository userLookupRepository;

    @Transactional(readOnly = true)
    public List<ReseauMembreDTO> listMembres(UUID reseauId, UUID userId) {
        requireMembership(reseauId, userId);
        requireActiveReseau(reseauId);

        List<ReseauMembreEntity> membres = membreAdminRepository.findByReseauIdOrderByCreatedAtAsc(reseauId);
        List<UUID> userIds = membres.stream().map(ReseauMembreEntity::getUserId).toList();
        Map<UUID, UserEntity> users = userLookupRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<ReseauMembreDTO> result = new ArrayList<>();
        for (ReseauMembreEntity membre : membres) {
            UserEntity user = users.get(membre.getUserId());
            if (user == null) {
                continue;
            }
            result.add(new ReseauMembreDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getNom(),
                    user.getPrenom(),
                    membre.getRoleReseau(),
                    membre.getOrganisationId()
            ));
        }
        return result;
    }

    @Transactional
    public ReseauPosteDTO attachPoste(UUID reseauId, ReseauAttachPosteRequestDTO request, UUID userId) {
        requireAdminMembership(reseauId, userId);
        ReseauEntity reseau = requireActiveReseau(reseauId);

        String code = request.organisationCode().trim().toUpperCase();
        OrganisationEntity org = organisationLookupRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        "ORGANISATION_NOT_FOUND", "Organisation introuvable: " + code, HttpStatus.NOT_FOUND));

        if (Boolean.FALSE.equals(org.getActif())) {
            throw new BusinessException(
                    "ORGANISATION_INACTIVE", "Organisation inactive", HttpStatus.BAD_REQUEST);
        }

        if (posteAdminRepository.existsByReseauIdAndOrganisationId(reseauId, org.getId())) {
            throw new BusinessException(
                    "POSTE_ALREADY_ATTACHED", "Poste déjà rattaché à ce réseau", HttpStatus.CONFLICT);
        }

        ReseauPosteEntity poste = new ReseauPosteEntity();
        poste.setReseauId(reseau.getId());
        poste.setOrganisationId(org.getId());
        posteAdminRepository.save(poste);

        log.info("Poste {} rattaché au réseau {}", org.getCode(), reseau.getCode());
        return new ReseauPosteDTO(org.getId(), org.getCode(), org.getNom(), org.getPaysCode(), org.getActif());
    }

    @Transactional
    public void detachPoste(UUID reseauId, UUID organisationId, UUID userId) {
        requireAdminMembership(reseauId, userId);
        requireActiveReseau(reseauId);

        ReseauPosteEntity poste = posteAdminRepository
                .findByReseauIdAndOrganisationId(reseauId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "POSTE_NOT_ATTACHED", "Poste non rattaché à ce réseau", HttpStatus.NOT_FOUND));

        posteAdminRepository.delete(poste);
        log.info("Poste {} détaché du réseau {}", organisationId, reseauId);
    }

    private ReseauEntity requireActiveReseau(UUID reseauId) {
        return reseauRepository.findById(reseauId)
                .filter(r -> Boolean.TRUE.equals(r.getActif()))
                .orElseThrow(() -> new BusinessException(
                        "RESEAU_NOT_FOUND", "Réseau introuvable", HttpStatus.NOT_FOUND));
    }

    private void requireMembership(UUID reseauId, UUID userId) {
        if (!membreRepository.existsByReseauIdAndUserId(reseauId, userId)) {
            throw new BusinessException(
                    "RESEAU_FORBIDDEN", "Accès réseau non autorisé", HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdminMembership(UUID reseauId, UUID userId) {
        ReseauMembreEntity membre = membreRepository.findByReseauIdAndUserId(reseauId, userId)
                .orElseThrow(() -> new BusinessException(
                        "RESEAU_FORBIDDEN", "Accès réseau non autorisé", HttpStatus.FORBIDDEN));
        if (!ROLE_ADMIN.equalsIgnoreCase(membre.getRoleReseau())) {
            throw new BusinessException(
                    "RESEAU_ADMIN_REQUIRED", "Rôle ADMIN réseau requis", HttpStatus.FORBIDDEN);
        }
    }
}
