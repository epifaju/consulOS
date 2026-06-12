package com.consulos.rendezvous;

import com.consulos.common.BusinessException;
import com.consulos.rendezvous.dto.ConfigCreneauDTO;
import com.consulos.rendezvous.dto.JourFermetureDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParametresService {

    private final ConfigCreneauRepository configCreneauRepository;
    private final JourFermetureRepository jourFermetureRepository;

    @Transactional(readOnly = true)
    public List<ConfigCreneauDTO> listCreneaux(UUID organisationId) {
        return configCreneauRepository.findByOrganisationId(organisationId).stream()
                .map(this::toCreneauDto)
                .toList();
    }

    @Transactional
    public ConfigCreneauDTO createCreneau(ConfigCreneauDTO dto, UUID organisationId) {
        ConfigCreneauEntity entity = new ConfigCreneauEntity();
        entity.setOrganisationId(organisationId);
        applyCreneau(entity, dto);
        return toCreneauDto(configCreneauRepository.save(entity));
    }

    @Transactional
    public ConfigCreneauDTO updateCreneau(UUID id, ConfigCreneauDTO dto, UUID organisationId) {
        ConfigCreneauEntity entity = configCreneauRepository.findById(id)
                .filter(c -> organisationId.equals(c.getOrganisationId()))
                .orElseThrow(() -> new BusinessException(
                        "CRENEAU_NOT_FOUND", "Configuration créneau introuvable", HttpStatus.NOT_FOUND));
        applyCreneau(entity, dto);
        return toCreneauDto(configCreneauRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<JourFermetureDTO> listJoursFermeture(UUID organisationId) {
        return jourFermetureRepository.findByOrganisationIdOrderByDateFermetureAsc(organisationId).stream()
                .map(this::toFermetureDto)
                .toList();
    }

    @Transactional
    public JourFermetureDTO createJourFermeture(JourFermetureDTO dto, UUID organisationId) {
        if (jourFermetureRepository.existsByOrganisationIdAndDateFermeture(organisationId, dto.dateFermeture())) {
            throw new BusinessException(
                    "FERMETURE_EXISTANTE", "Cette date de fermeture existe déjà", HttpStatus.CONFLICT);
        }
        JourFermetureEntity entity = new JourFermetureEntity();
        entity.setOrganisationId(organisationId);
        entity.setDateFermeture(dto.dateFermeture());
        entity.setMotif(dto.motif());
        return toFermetureDto(jourFermetureRepository.save(entity));
    }

    @Transactional
    public JourFermetureDTO updateJourFermeture(UUID id, JourFermetureDTO dto, UUID organisationId) {
        JourFermetureEntity entity = jourFermetureRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "FERMETURE_NOT_FOUND", "Jour de fermeture introuvable", HttpStatus.NOT_FOUND));
        entity.setDateFermeture(dto.dateFermeture());
        entity.setMotif(dto.motif());
        return toFermetureDto(jourFermetureRepository.save(entity));
    }

    private void applyCreneau(ConfigCreneauEntity entity, ConfigCreneauDTO dto) {
        entity.setTypeServiceId(dto.typeServiceId());
        entity.setJourSemaine(dto.jourSemaine());
        entity.setHeureDebut(dto.heureDebut());
        entity.setHeureFin(dto.heureFin());
        entity.setDureeMinutes(dto.dureeMinutes() != null ? dto.dureeMinutes() : 30);
        entity.setQuotaMax(dto.quotaMax() != null ? dto.quotaMax() : 10);
        entity.setActif(dto.actif() != null ? dto.actif() : true);
    }

    private ConfigCreneauDTO toCreneauDto(ConfigCreneauEntity entity) {
        return new ConfigCreneauDTO(
                entity.getId(),
                entity.getTypeServiceId(),
                entity.getJourSemaine(),
                entity.getHeureDebut(),
                entity.getHeureFin(),
                entity.getDureeMinutes(),
                entity.getQuotaMax(),
                entity.getActif()
        );
    }

    private JourFermetureDTO toFermetureDto(JourFermetureEntity entity) {
        return new JourFermetureDTO(entity.getId(), entity.getDateFermeture(), entity.getMotif());
    }
}
