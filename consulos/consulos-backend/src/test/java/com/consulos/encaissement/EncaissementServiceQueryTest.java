package com.consulos.encaissement;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Requêtes non couvertes par EncaissementServiceTest : findById, list, getRecu.
 */
@ExtendWith(MockitoExtension.class)
class EncaissementServiceQueryTest {

    @Mock
    private EncaissementRepository encaissementRepository;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private RecuPdfService recuPdfService;

    @InjectMocks
    private EncaissementService encaissementService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID usagerId;
    private UUID encaissementId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        encaissementId = UUID.randomUUID();
    }

    @Test
    void findById_success_shouldReturnDetail() {
        EncaissementEntity enc = buildEncaissement();
        UsagerEntity usager = activeUsager();

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = encaissementService.findById(encaissementId, organisationId);

        assertThat(result.id()).isEqualTo(encaissementId);
        assertThat(result.numeroRecu()).isEqualTo("REC-2026-00042");
        assertThat(result.usagerNom()).isEqualTo("Camara");
        assertThat(result.statut()).isEqualTo(StatutEncaissement.DU);
        assertThat(result.montantTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void findById_wrongOrg_shouldThrowEncaissementNotFound() {
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, otherOrganisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> encaissementService.findById(encaissementId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ENCAISSEMENT_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void list_shouldMapSummaryWithNumeroRecu() {
        EncaissementEntity enc = buildEncaissement();
        UsagerEntity usager = activeUsager();
        Pageable pageable = PageRequest.of(0, 20);

        when(encaissementRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(enc), pageable, 1));
        when(usagerRepository.findAllById(List.of(usagerId))).thenReturn(List.of(usager));

        var page = encaissementService.list(organisationId, null, null, null, null, null, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().numeroRecu()).isEqualTo("REC-2026-00042");
        assertThat(page.content().getFirst().usagerNom()).isEqualTo("Camara");
        assertThat(page.content().getFirst().statut()).isEqualTo(StatutEncaissement.DU);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void getRecu_success_shouldReturnPdfBytes() {
        EncaissementEntity enc = buildEncaissement();
        UsagerEntity usager = activeUsager();
        OrganisationEntity org = new OrganisationEntity();
        org.setId(organisationId);
        org.setCode("GNB");
        org.setNom("Ambassade");
        org.setPaysCode("GNB");
        org.setActif(true);
        byte[] pdf = new byte[]{1, 2, 3, 4};

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(org));
        when(recuPdfService.generateRecu(eq(enc), eq(usager), isNull(), isNull(), eq(org), isNull()))
                .thenReturn(pdf);

        byte[] result = encaissementService.getRecu(encaissementId, organisationId);

        assertThat(result).isEqualTo(pdf);
        verify(recuPdfService).generateRecu(eq(enc), eq(usager), isNull(), isNull(), eq(org), isNull());
    }

    @Test
    void getRecu_whenOrgMissing_shouldThrowOrgNotFound() {
        EncaissementEntity enc = buildEncaissement();
        UsagerEntity usager = activeUsager();

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encaissementService.getRecu(encaissementId, organisationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ORG_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        usager.setNumero("USG-042");
        return usager;
    }

    private EncaissementEntity buildEncaissement() {
        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(encaissementId);
        enc.setOrganisationId(organisationId);
        enc.setUsagerId(usagerId);
        enc.setNumeroRecu("REC-2026-00042");
        enc.setMontantTotal(new BigDecimal("100.00"));
        enc.setMontantPercu(BigDecimal.ZERO);
        enc.setDevise("EUR");
        enc.setStatut(StatutEncaissement.DU);
        return enc;
    }
}
