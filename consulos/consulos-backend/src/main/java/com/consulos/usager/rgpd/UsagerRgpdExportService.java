package com.consulos.usager.rgpd;

import com.consulos.audit.Auditable;
import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.document.DocumentEntity;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.rendezvous.RendezvousEntity;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerMapper;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.rgpd.dto.UsagerRgpdExportDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsagerRgpdExportService {

    private static final String EXPORT_VERSION = "1.0";

    private final UsagerRepository usagerRepository;
    private final UsagerMapper usagerMapper;
    private final DossierRepository dossierRepository;
    private final TypeServiceRepository typeServiceRepository;
    private final UsagerRgpdRelatedDataRepository relatedDataRepository;

    @Transactional(readOnly = true)
    @Auditable(action = "USAGER_RGPD_EXPORT")
    public byte[] exportJson(UUID usagerId, UUID organisationId) {
        UsagerRgpdExportDTO payload = buildExport(usagerId, organisationId);
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        } catch (Exception ex) {
            throw new BusinessException(
                    "RGPD_EXPORT_FAILED",
                    "Impossible de générer l'export RGPD",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Transactional(readOnly = true)
    public UsagerRgpdExportDTO buildExport(UUID usagerId, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(usagerId)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId()) || !Boolean.TRUE.equals(usager.getActif())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }

        var detail = usagerMapper.toDetail(usager);
        UsagerRgpdExportDTO.UsagerRgpdProfileDTO profile = new UsagerRgpdExportDTO.UsagerRgpdProfileDTO(
                detail.id(),
                detail.numero(),
                detail.nom(),
                detail.prenom(),
                detail.dateNaissance() != null ? detail.dateNaissance().toString() : null,
                detail.lieuNaissance(),
                detail.nationalite(),
                detail.typeUsager() != null ? detail.typeUsager().name() : null,
                detail.telephone(),
                detail.email(),
                detail.adresse(),
                detail.numeroDocument(),
                detail.typeDocument() != null ? detail.typeDocument().name() : null,
                detail.metadata(),
                detail.actif(),
                detail.createdAt() != null ? detail.createdAt().toString() : null,
                detail.updatedAt() != null ? detail.updatedAt().toString() : null
        );

        List<DossierEntity> dossiers = dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId);
        Map<UUID, TypeServiceEntity> types = loadTypes(dossiers);

        List<UsagerRgpdExportDTO.UsagerRgpdDossierDTO> dossierDtos = dossiers.stream()
                .map(d -> {
                    TypeServiceEntity type = types.get(d.getTypeServiceId());
                    return new UsagerRgpdExportDTO.UsagerRgpdDossierDTO(
                            d.getId(),
                            d.getReference(),
                            d.getStatut() != null ? d.getStatut().name() : null,
                            d.getPriorite() != null ? d.getPriorite().name() : null,
                            type != null ? type.getLibelle() : null,
                            d.getDateSoumission() != null ? d.getDateSoumission().toString() : null,
                            d.getDateEcheance() != null ? d.getDateEcheance().toString() : null,
                            d.getCreatedAt() != null ? d.getCreatedAt().toString() : null
                    );
                })
                .toList();

        List<UsagerRgpdExportDTO.UsagerRgpdRendezvousDTO> rdvDtos = relatedDataRepository
                .findRendezvousByUsager(organisationId, usagerId)
                .stream()
                .map(this::toRdvDto)
                .toList();

        List<UsagerRgpdExportDTO.UsagerRgpdEncaissementDTO> encDtos = relatedDataRepository
                .findEncaissementsByUsager(organisationId, usagerId)
                .stream()
                .map(this::toEncaissementDto)
                .toList();

        List<UsagerRgpdExportDTO.UsagerRgpdDocumentDTO> docDtos = relatedDataRepository
                .findDocumentsByUsager(organisationId, usagerId)
                .stream()
                .map(this::toDocumentDto)
                .toList();

        return new UsagerRgpdExportDTO(
                OffsetDateTime.now(),
                EXPORT_VERSION,
                profile,
                dossierDtos,
                rdvDtos,
                encDtos,
                docDtos
        );
    }

    public String suggestedFilename(UsagerEntity usager) {
        String safeNumero = usager.getNumero() != null ? usager.getNumero().replaceAll("[^A-Za-z0-9_-]", "_") : usager.getId().toString();
        return "rgpd-" + safeNumero + ".json";
    }

    public String suggestedFilename(UUID usagerId, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(usagerId)
                .orElseThrow(() -> new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));
        if (!organisationId.equals(usager.getOrganisationId())) {
            throw new BusinessException("USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND);
        }
        return suggestedFilename(usager);
    }

    private Map<UUID, TypeServiceEntity> loadTypes(List<DossierEntity> dossiers) {
        List<UUID> ids = dossiers.stream().map(DossierEntity::getTypeServiceId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return typeServiceRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TypeServiceEntity::getId, Function.identity()));
    }

    private UsagerRgpdExportDTO.UsagerRgpdRendezvousDTO toRdvDto(RendezvousEntity r) {
        return new UsagerRgpdExportDTO.UsagerRgpdRendezvousDTO(
                r.getId(),
                r.getDateHeure() != null ? r.getDateHeure().toString() : null,
                r.getStatut() != null ? r.getStatut().name() : null,
                r.getMotif(),
                r.getDureeMinutes(),
                null
        );
    }

    private UsagerRgpdExportDTO.UsagerRgpdEncaissementDTO toEncaissementDto(EncaissementEntity e) {
        return new UsagerRgpdExportDTO.UsagerRgpdEncaissementDTO(
                e.getId(),
                e.getNumeroRecu(),
                e.getMontantTotal() != null ? e.getMontantTotal().toPlainString() : null,
                e.getMontantPercu() != null ? e.getMontantPercu().toPlainString() : null,
                e.getDevise(),
                e.getStatut() != null ? e.getStatut().name() : null,
                e.getModePaiement() != null ? e.getModePaiement().name() : null,
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
        );
    }

    private UsagerRgpdExportDTO.UsagerRgpdDocumentDTO toDocumentDto(DocumentEntity d) {
        return new UsagerRgpdExportDTO.UsagerRgpdDocumentDTO(
                d.getId(),
                d.getNomOriginal() != null ? d.getNomOriginal() : d.getNomFichier(),
                d.getMimeType(),
                d.getTailleOctets(),
                d.getTypeDocument(),
                d.getCreatedAt() != null ? d.getCreatedAt().toString() : null
        );
    }
}
