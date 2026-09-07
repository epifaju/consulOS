package com.consulos.portal;

import com.consulos.common.ApiResponse;
import com.consulos.common.BusinessException;
import com.consulos.portal.dto.PortalAccessRequestDTO;
import com.consulos.portal.dto.PortalDossierSummaryDTO;
import com.consulos.portal.dto.PortalDocumentDTO;
import com.consulos.portal.dto.PortalRdvCreateDTO;
import com.consulos.portal.dto.PortalSessionRequestDTO;
import com.consulos.portal.dto.PortalSessionResponseDTO;
import com.consulos.rendezvous.dto.CreneauDTO;
import com.consulos.rendezvous.dto.RdvDetailDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalAccessService portalAccessService;
    private final PortalDocumentService portalDocumentService;
    private final PortalRdvService portalRdvService;

    @PostMapping("/access")
    public ApiResponse<Map<String, String>> requestAccess(@Valid @RequestBody PortalAccessRequestDTO dto) {
        portalAccessService.requestAccess(dto.reference(), dto.email());
        return ApiResponse.ok(
                Map.of("status", "accepted"),
                "Si les informations correspondent, un email d'accès a été envoyé."
        );
    }

    @PostMapping("/session")
    public ApiResponse<PortalSessionResponseDTO> openSession(@Valid @RequestBody PortalSessionRequestDTO dto) {
        return ApiResponse.ok(portalAccessService.openSession(dto.token()));
    }

    @GetMapping("/dossier")
    public ApiResponse<PortalDossierSummaryDTO> dossier() {
        return ApiResponse.ok(portalAccessService.getDossierSummary(requireSession()));
    }

    @GetMapping("/documents")
    public ApiResponse<List<PortalDocumentDTO>> documents() {
        return ApiResponse.ok(portalDocumentService.list(requireSession()));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PortalDocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "typeDocument", defaultValue = "AUTRE") String typeDocument,
            @RequestParam(value = "notes", required = false) String notes
    ) {
        return ApiResponse.ok(portalDocumentService.upload(requireSession(), file, typeDocument, notes));
    }

    @GetMapping("/disponibilites")
    public ApiResponse<List<CreneauDTO>> disponibilites(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(portalRdvService.disponibilites(requireSession(), date));
    }

    @PostMapping("/rendezvous")
    public ApiResponse<RdvDetailDTO> createRendezVous(@Valid @RequestBody PortalRdvCreateDTO dto) {
        return ApiResponse.ok(portalRdvService.create(requireSession(), dto));
    }

    private PortalSession requireSession() {
        PortalSession session = PortalSessionHolder.get();
        if (session == null) {
            throw new BusinessException(
                    "PORTAL_SESSION_REQUIRED", "Session portail requise", HttpStatus.UNAUTHORIZED);
        }
        return session;
    }
}
