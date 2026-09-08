package com.consulos.reseau;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.common.BusinessException;
import com.consulos.reseau.dto.ReseauMeDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import com.consulos.reseau.dto.ReseauSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.reseau", name = "enabled", havingValue = "true")
public class ReseauService {

    private final ReseauRepository reseauRepository;
    private final ReseauMembreRepository membreRepository;
    private final ReseauPosteRepository posteRepository;
    private final ReseauOrganisationLookupRepository organisationLookupRepository;
    private final ReseauProperties properties;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Transactional(readOnly = true)
    public ReseauMeDTO getMyReseaux(UUID userId) {
        List<ReseauMembreEntity> memberships = membreRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return new ReseauMeDTO(List.of());
        }

        Map<UUID, ReseauMembreEntity> membershipByReseau = memberships.stream()
                .collect(Collectors.toMap(ReseauMembreEntity::getReseauId, Function.identity(), (a, b) -> a));

        List<UUID> reseauIds = new ArrayList<>(membershipByReseau.keySet());
        List<ReseauEntity> reseaux = reseauRepository.findByIdInAndActifTrue(reseauIds);
        List<ReseauPosteEntity> allPostes = posteRepository.findByReseauIdIn(reseauIds);

        Set<UUID> orgIds = allPostes.stream()
                .map(ReseauPosteEntity::getOrganisationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, OrganisationEntity> orgsById = organisationLookupRepository.findByIdIn(orgIds).stream()
                .collect(Collectors.toMap(OrganisationEntity::getId, Function.identity()));

        Map<UUID, List<ReseauPosteEntity>> postesByReseau = allPostes.stream()
                .collect(Collectors.groupingBy(ReseauPosteEntity::getReseauId));

        List<ReseauSummaryDTO> summaries = reseaux.stream()
                .map(reseau -> {
                    ReseauMembreEntity membre = membershipByReseau.get(reseau.getId());
                    List<ReseauPosteDTO> postes = postesByReseau
                            .getOrDefault(reseau.getId(), List.of())
                            .stream()
                            .map(p -> toPosteDto(orgsById.get(p.getOrganisationId())))
                            .filter(dto -> dto != null)
                            .toList();
                    return new ReseauSummaryDTO(
                            reseau.getId(),
                            reseau.getCode(),
                            reseau.getNom(),
                            reseau.getDescription(),
                            membre != null ? membre.getRoleReseau() : null,
                            postes
                    );
                })
                .toList();

        return new ReseauMeDTO(summaries);
    }

    @Transactional(readOnly = true)
    public List<ReseauPosteDTO> listPostes(UUID reseauId, UUID userId) {
        requireMembership(reseauId, userId);
        ReseauEntity reseau = reseauRepository.findById(reseauId)
                .filter(r -> Boolean.TRUE.equals(r.getActif()))
                .orElseThrow(() -> new BusinessException(
                        "RESEAU_NOT_FOUND", "Réseau introuvable", HttpStatus.NOT_FOUND));

        List<ReseauPosteEntity> postes = posteRepository.findByReseauId(reseau.getId());
        Set<UUID> orgIds = postes.stream()
                .map(ReseauPosteEntity::getOrganisationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, OrganisationEntity> orgsById = organisationLookupRepository.findByIdIn(orgIds).stream()
                .collect(Collectors.toMap(OrganisationEntity::getId, Function.identity()));

        return postes.stream()
                .map(p -> toPosteDto(orgsById.get(p.getOrganisationId())))
                .filter(dto -> dto != null)
                .toList();
    }

    /**
     * Périmètre d'organisations autorisées pour un utilisateur sur un réseau donné.
     * Utilisé par Vague B (dashboard régional).
     */
    @Transactional(readOnly = true)
    public Set<UUID> resolveAllowedOrganisationIds(UUID reseauId, UUID userId) {
        requireMembership(reseauId, userId);
        return posteRepository.findByReseauId(reseauId).stream()
                .map(ReseauPosteEntity::getOrganisationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Union des organisations de tous les réseaux dont l'utilisateur est membre.
     */
    @Transactional(readOnly = true)
    public Set<UUID> resolveAllAllowedOrganisationIds(UUID userId) {
        List<ReseauMembreEntity> memberships = membreRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return Set.of();
        }
        List<UUID> reseauIds = memberships.stream().map(ReseauMembreEntity::getReseauId).toList();
        return posteRepository.findByReseauIdIn(reseauIds).stream()
                .map(ReseauPosteEntity::getOrganisationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void requireMembership(UUID reseauId, UUID userId) {
        if (!membreRepository.existsByReseauIdAndUserId(reseauId, userId)) {
            throw new BusinessException(
                    "RESEAU_FORBIDDEN", "Accès réseau non autorisé", HttpStatus.FORBIDDEN);
        }
    }

    private ReseauPosteDTO toPosteDto(OrganisationEntity org) {
        if (org == null) {
            return null;
        }
        return new ReseauPosteDTO(
                org.getId(),
                org.getCode(),
                org.getNom(),
                org.getPaysCode(),
                org.getActif()
        );
    }
}
