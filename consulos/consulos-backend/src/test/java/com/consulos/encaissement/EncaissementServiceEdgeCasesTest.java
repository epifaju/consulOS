package com.consulos.encaissement;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.dto.EncaissementCreateDTO;
import com.consulos.encaissement.dto.PaiementDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Écarts non couverts par EncaissementServiceTest : EXONERE, REMBOURSE, montant excessif.
 */
@ExtendWith(MockitoExtension.class)
class EncaissementServiceEdgeCasesTest {

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
    private UUID usagerId;
    private UUID caissierId;
    private UUID encaissementId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        caissierId = UUID.randomUUID();
        encaissementId = UUID.randomUUID();
    }

    @Test
    void create_withModeExonere_shouldSetStatutExonere() {
        UsagerEntity usager = activeUsager();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(encaissementRepository.nextEncaissementSequence()).thenReturn(42L);
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(invocation -> {
            EncaissementEntity entity = invocation.getArgument(0);
            entity.setId(encaissementId);
            return entity;
        });

        var result = encaissementService.create(
                new EncaissementCreateDTO(usagerId, null, new BigDecimal("80.00"), "EUR", ModePaiement.EXONERE, "aide"),
                caissierId,
                organisationId
        );

        ArgumentCaptor<EncaissementEntity> captor = ArgumentCaptor.forClass(EncaissementEntity.class);
        verify(encaissementRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutEncaissement.EXONERE);
        assertThat(captor.getValue().getModePaiement()).isEqualTo(ModePaiement.EXONERE);
        assertThat(result.statut()).isEqualTo(StatutEncaissement.EXONERE);
        assertThat(result.modePaiement()).isEqualTo(ModePaiement.EXONERE);
    }

    @Test
    void enregistrerPaiement_whenExonere_shouldReject() {
        EncaissementEntity enc = buildEncaissement(StatutEncaissement.EXONERE);
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));

        assertThatThrownBy(() -> encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("10.00"), ModePaiement.ESPECES, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("PAIEMENT_NON_AUTORISE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(encaissementRepository, never()).save(any());
    }

    @Test
    void enregistrerPaiement_whenRembourse_shouldReject() {
        EncaissementEntity enc = buildEncaissement(StatutEncaissement.REMBOURSE);
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));

        assertThatThrownBy(() -> encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("10.00"), ModePaiement.CARTE, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("PAIEMENT_NON_AUTORISE"));
    }

    @Test
    void enregistrerPaiement_whenMontantExceedsResteDu_shouldReject() {
        EncaissementEntity enc = buildEncaissement(StatutEncaissement.DU);
        enc.setMontantTotal(new BigDecimal("100.00"));
        enc.setMontantPercu(new BigDecimal("70.00"));
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));

        assertThatThrownBy(() -> encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("40.00"), ModePaiement.ESPECES, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("MONTANT_EXCESSIF");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void create_whenDossierUsagerMismatch_shouldReject() {
        UsagerEntity usager = activeUsager();
        UUID dossierId = UUID.randomUUID();
        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(UUID.randomUUID());

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));

        assertThatThrownBy(() -> encaissementService.create(
                new EncaissementCreateDTO(usagerId, dossierId, new BigDecimal("50.00"), "EUR", null, null),
                caissierId,
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOSSIER_USAGER_MISMATCH"));

        verify(encaissementRepository, never()).save(any());
    }

    private UsagerEntity activeUsager() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Mamadou");
        usager.setNumero("USG-010");
        return usager;
    }

    private EncaissementEntity buildEncaissement(StatutEncaissement statut) {
        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(encaissementId);
        enc.setOrganisationId(organisationId);
        enc.setUsagerId(usagerId);
        enc.setNumeroRecu("REC-2026-00099");
        enc.setMontantTotal(new BigDecimal("100.00"));
        enc.setMontantPercu(BigDecimal.ZERO);
        enc.setDevise("EUR");
        enc.setStatut(statut);
        return enc;
    }
}
