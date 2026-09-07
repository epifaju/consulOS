package com.consulos.portal;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.rendezvous.DisponibiliteService;
import com.consulos.rendezvous.RdvService;
import com.consulos.rendezvous.dto.CreneauDTO;
import com.consulos.rendezvous.dto.RdvCreateDTO;
import com.consulos.rendezvous.dto.RdvDetailDTO;
import com.consulos.portal.dto.PortalRdvCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortalRdvService {

    private final DisponibiliteService disponibiliteService;
    private final RdvService rdvService;
    private final PortalDossierLookupRepository dossierLookupRepository;

    @Transactional(readOnly = true)
    public List<CreneauDTO> disponibilites(PortalSession session, LocalDate date) {
        DossierEntity dossier = requireDossier(session);
        return disponibiliteService.getDisponibilites(
                session.organisationId(),
                dossier.getTypeServiceId(),
                date
        );
    }

    @Transactional
    public RdvDetailDTO create(PortalSession session, PortalRdvCreateDTO dto) {
        DossierEntity dossier = requireDossier(session);
        RdvCreateDTO createDto = new RdvCreateDTO(
                session.usagerId(),
                dto.dateHeure(),
                dossier.getTypeServiceId(),
                session.dossierId(),
                null,
                dto.dureeMinutes() != null ? dto.dureeMinutes() : 30,
                dto.notes()
        );
        return rdvService.create(createDto, session.organisationId());
    }

    private DossierEntity requireDossier(PortalSession session) {
        return dossierLookupRepository.findById(session.dossierId())
                .filter(d -> Boolean.TRUE.equals(d.getActif())
                        && session.organisationId().equals(d.getOrganisationId()))
                .orElseThrow(() -> new BusinessException(
                        "DOSSIER_NOT_FOUND", "Dossier introuvable", HttpStatus.NOT_FOUND));
    }
}
