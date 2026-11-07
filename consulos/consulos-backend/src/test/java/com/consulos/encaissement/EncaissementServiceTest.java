package com.consulos.encaissement;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncaissementServiceTest {

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
    void create_shouldGenerateRecNumberAndDuStatus() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Aminata");
        usager.setNumero("USG-001");

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(encaissementRepository.nextEncaissementSequence()).thenReturn(7L);
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(invocation -> {
            EncaissementEntity entity = invocation.getArgument(0);
            entity.setId(encaissementId);
            return entity;
        });

        var result = encaissementService.create(
                new EncaissementCreateDTO(usagerId, null, new BigDecimal("50.00"), "EUR", null, null),
                caissierId,
                organisationId
        );

        ArgumentCaptor<EncaissementEntity> captor = ArgumentCaptor.forClass(EncaissementEntity.class);
        verify(encaissementRepository).save(captor.capture());
        EncaissementEntity saved = captor.getValue();

        assertThat(saved.getNumeroRecu()).matches("REC-\\d{4}-00007");
        assertThat(saved.getStatut()).isEqualTo(StatutEncaissement.DU);
        assertThat(saved.getMontantPercu()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.numeroRecu()).matches("REC-\\d{4}-00007");
        assertThat(result.statut()).isEqualTo(StatutEncaissement.DU);
    }

    @Test
    void enregistrerPaiement_complet_shouldSetPaye() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);

        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Diallo");
        usager.setPrenom("Fatou");
        usager.setNumero("USG-002");

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("100.00"), ModePaiement.CARTE, null),
                organisationId
        );

        assertThat(result.statut()).isEqualTo(StatutEncaissement.PAYE);
        assertThat(result.montantPercu()).isEqualByComparingTo("100.00");
        assertThat(result.modePaiement()).isEqualTo(ModePaiement.CARTE);
    }

    @Test
    void enregistrerPaiement_partiel_shouldSetPartiel() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);

        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Sow");
        usager.setPrenom("Ibrahima");
        usager.setNumero("USG-003");

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("40.00"), ModePaiement.ESPECES, null),
                organisationId
        );

        assertThat(result.statut()).isEqualTo(StatutEncaissement.PARTIEL);
        assertThat(result.montantPercu()).isEqualByComparingTo("40.00");
    }

    @Test
    void getJournalCaisse_shouldAggregateByMode() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        OffsetDateTime debut = EncaissementDayBounds.startOfDay(date);
        OffsetDateTime fin = EncaissementDayBounds.startOfNextDay(date);

        when(encaissementRepository.sumByModeForDay(eq(organisationId), eq(debut), eq(fin)))
                .thenReturn(List.of(
                        new Object[]{ModePaiement.ESPECES, 5L, new BigDecimal("430.00")},
                        new Object[]{ModePaiement.CARTE, 2L, new BigDecimal("172.00")}
                ));

        var journal = encaissementService.getJournalCaisse(date, organisationId);

        assertThat(journal.nombreTransactions()).isEqualTo(7);
        assertThat(journal.montantTotalPercu()).isEqualByComparingTo("602.00");
        assertThat(journal.parMode()).hasSize(2);
        assertThat(journal.parMode().get(0).modePaiement()).isEqualTo("ESPECES");
        assertThat(journal.parMode().get(0).nombreTransactions()).isEqualTo(5);
        assertThat(journal.parMode().get(0).total()).isEqualByComparingTo("430.00");
    }

    private EncaissementEntity buildEncaissement(BigDecimal total, BigDecimal percu) {
        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(encaissementId);
        enc.setOrganisationId(organisationId);
        enc.setUsagerId(usagerId);
        enc.setNumeroRecu("REC-2026-00001");
        enc.setMontantTotal(total);
        enc.setMontantPercu(percu);
        enc.setDevise("EUR");
        enc.setStatut(StatutEncaissement.DU);
        return enc;
    }
}
