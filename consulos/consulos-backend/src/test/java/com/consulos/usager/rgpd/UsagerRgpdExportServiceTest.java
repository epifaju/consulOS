package com.consulos.usager.rgpd;

import com.consulos.common.BusinessException;
import com.consulos.document.DocumentEntity;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierPriorite;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.DossierStatut;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.encaissement.ModePaiement;
import com.consulos.encaissement.StatutEncaissement;
import com.consulos.rendezvous.RendezvousEntity;
import com.consulos.rendezvous.RdvStatut;
import com.consulos.usager.TypeUsager;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerMapper;
import com.consulos.usager.UsagerRepository;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.rgpd.dto.UsagerRgpdExportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsagerRgpdExportServiceTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private UsagerMapper usagerMapper;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private UsagerRgpdRelatedDataRepository relatedDataRepository;

    @InjectMocks
    private UsagerRgpdExportService usagerRgpdExportService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID usagerId;
    private UsagerEntity usager;
    private UsagerDetailDTO detail;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();

        usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setNumero("GNB-2026-00001");
        usager.setNom("Camara");
        usager.setPrenom("Mamadou");
        usager.setActif(true);

        detail = new UsagerDetailDTO(
                usagerId,
                "GNB-2026-00001",
                "Camara",
                "Mamadou",
                LocalDate.of(1990, 5, 1),
                "Conakry",
                "GNB",
                TypeUsager.DIASPORA,
                "+33600000000",
                "mamadou@email.com",
                "Paris",
                null,
                null,
                Map.of(),
                true,
                organisationId,
                LocalDateTime.parse("2026-01-01T10:00:00"),
                LocalDateTime.parse("2026-01-02T10:00:00")
        );
    }

    @Test
    void buildExport_shouldAggregateRelatedDataForOrganisation() {
        UUID typeServiceId = UUID.randomUUID();
        DossierEntity dossier = new DossierEntity();
        dossier.setId(UUID.randomUUID());
        dossier.setReference("DOS-001");
        dossier.setTypeServiceId(typeServiceId);
        dossier.setStatut(DossierStatut.SOUMIS);
        dossier.setPriorite(DossierPriorite.NORMALE);
        dossier.setCreatedAt(LocalDateTime.parse("2026-02-01T12:00:00"));

        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setLibelle("Passeport");

        RendezvousEntity rdv = new RendezvousEntity();
        rdv.setId(UUID.randomUUID());
        rdv.setDateHeure(OffsetDateTime.parse("2026-03-01T09:00:00Z"));
        rdv.setStatut(RdvStatut.CONFIRME);
        rdv.setMotif("Remise");
        rdv.setDureeMinutes(30);

        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(UUID.randomUUID());
        enc.setNumeroRecu("REC-2026-00001");
        enc.setMontantTotal(new BigDecimal("85.00"));
        enc.setMontantPercu(new BigDecimal("85.00"));
        enc.setDevise("EUR");
        enc.setStatut(StatutEncaissement.PAYE);
        enc.setModePaiement(ModePaiement.CARTE);
        enc.setCreatedAt(OffsetDateTime.parse("2026-03-02T10:00:00Z"));

        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setNomOriginal("scan.pdf");
        doc.setMimeType("application/pdf");
        doc.setTailleOctets(2048L);
        doc.setTypeDocument("PASSEPORT");
        doc.setCreatedAt(OffsetDateTime.parse("2026-03-03T11:00:00Z"));

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(usagerMapper.toDetail(usager)).thenReturn(detail);
        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of(dossier));
        when(typeServiceRepository.findAllById(List.of(typeServiceId))).thenReturn(List.of(type));
        when(relatedDataRepository.findRendezvousByUsager(organisationId, usagerId)).thenReturn(List.of(rdv));
        when(relatedDataRepository.findEncaissementsByUsager(organisationId, usagerId)).thenReturn(List.of(enc));
        when(relatedDataRepository.findDocumentsByUsager(organisationId, usagerId)).thenReturn(List.of(doc));

        UsagerRgpdExportDTO export = usagerRgpdExportService.buildExport(usagerId, organisationId);

        assertThat(export.exportVersion()).isEqualTo("1.0");
        assertThat(export.usager().numero()).isEqualTo("GNB-2026-00001");
        assertThat(export.usager().email()).isEqualTo("mamadou@email.com");
        assertThat(export.dossiers()).hasSize(1);
        assertThat(export.dossiers().getFirst().typeServiceLibelle()).isEqualTo("Passeport");
        assertThat(export.rendezvous()).hasSize(1);
        assertThat(export.encaissements().getFirst().numeroRecu()).isEqualTo("REC-2026-00001");
        assertThat(export.documents().getFirst().nomFichier()).isEqualTo("scan.pdf");
    }

    @Test
    void buildExport_whenWrongOrganisation_shouldThrowNotFound() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> usagerRgpdExportService.buildExport(usagerId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void buildExport_whenInactive_shouldThrowNotFound() {
        usager.setActif(false);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> usagerRgpdExportService.buildExport(usagerId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));
    }

    @Test
    void exportJson_shouldReturnPrettyJsonBytes() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(usagerMapper.toDetail(usager)).thenReturn(detail);
        when(dossierRepository.findByUsagerIdAndOrganisationIdAndActifTrue(usagerId, organisationId))
                .thenReturn(List.of());
        when(relatedDataRepository.findRendezvousByUsager(organisationId, usagerId)).thenReturn(List.of());
        when(relatedDataRepository.findEncaissementsByUsager(organisationId, usagerId)).thenReturn(List.of());
        when(relatedDataRepository.findDocumentsByUsager(organisationId, usagerId)).thenReturn(List.of());

        byte[] json = usagerRgpdExportService.exportJson(usagerId, organisationId);

        String content = new String(json, StandardCharsets.UTF_8);
        assertThat(content).contains("\"exportVersion\" : \"1.0\"");
        assertThat(content).contains("GNB-2026-00001");
        assertThat(content).contains("mamadou@email.com");
    }

    @Test
    void suggestedFilename_shouldSanitizeNumero() {
        usager.setNumero("GNB/2026 00001");
        assertThat(usagerRgpdExportService.suggestedFilename(usager)).isEqualTo("rgpd-GNB_2026_00001.json");
    }

    @Test
    void suggestedFilename_whenWrongOrganisation_shouldThrow() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> usagerRgpdExportService.suggestedFilename(usagerId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USAGER_NOT_FOUND"));
    }
}
