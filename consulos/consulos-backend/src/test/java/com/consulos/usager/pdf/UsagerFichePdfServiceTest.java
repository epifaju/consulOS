package com.consulos.usager.pdf;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import com.consulos.usager.TypeDocument;
import com.consulos.usager.TypeUsager;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsagerFichePdfServiceTest {

    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private UsagerFichePdfService pdfService;

    private UUID organisationId;
    private UUID usagerId;
    private UsagerEntity usager;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();

        usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNumero("USG-2026-00001");
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        usager.setDateNaissance(LocalDate.of(1990, 5, 15));
        usager.setTypeUsager(TypeUsager.DIASPORA);
        usager.setTypeDocument(TypeDocument.PASSEPORT);
        usager.setCreatedAt(LocalDateTime.now());
        usager.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void generateFiche_shouldReturnValidPdf() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.empty());

        byte[] bytes = pdfService.generateFiche(usagerId, organisationId);

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateFiche_withOrganisation_shouldReturnValidPdf() {
        OrganisationEntity organisation = new OrganisationEntity();
        organisation.setId(organisationId);
        organisation.setNom("Consulat Général");
        organisation.setCode("GNB-FR");
        organisation.setPaysCode("GNB");

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));

        byte[] bytes = pdfService.generateFiche(usagerId, organisationId);

        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateFiche_whenWrongOrganisation_shouldThrowUsagerNotFound() {
        UUID otherOrg = UUID.randomUUID();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThatThrownBy(() -> pdfService.generateFiche(usagerId, otherOrg))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USAGER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void suggestedFilename_whenUsagerFound_shouldIncludeNumero() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        assertThat(pdfService.suggestedFilename(usagerId, organisationId))
                .isEqualTo("fiche-USG-2026-00001.pdf");
    }

    @Test
    void suggestedFilename_whenUsagerMissing_shouldUseDefault() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

        assertThat(pdfService.suggestedFilename(usagerId, organisationId))
                .isEqualTo("fiche-usager.pdf");
    }
}
