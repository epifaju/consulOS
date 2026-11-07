package com.consulos.encaissement;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Gaps remaining after EncaissementServiceTest / QueryTest / EdgeCases / CreateValidation.
 */
@ExtendWith(MockitoExtension.class)
class EncaissementServiceRemainingGapsTest {

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
    private UUID dossierId;
    private UUID typeServiceId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        caissierId = UUID.randomUUID();
        encaissementId = UUID.randomUUID();
        dossierId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
    }

    @Test
    void create_withMatchingDossier_shouldAttach() {
        UsagerEntity usager = activeUsager();
        DossierEntity dossier = dossier();
        TypeServiceEntity type = typeService();

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(encaissementRepository.nextEncaissementSequence()).thenReturn(11L);
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(inv -> {
            EncaissementEntity e = inv.getArgument(0);
            e.setId(encaissementId);
            return e;
        });
        when(typeServiceRepository.findById(typeServiceId)).thenReturn(Optional.of(type));

        var result = encaissementService.create(
                new EncaissementCreateDTO(usagerId, dossierId, new BigDecimal("80.00"), null, null, null),
                caissierId,
                organisationId
        );

        assertThat(result.dossierId()).isEqualTo(dossierId);
        assertThat(result.dossierReference()).isEqualTo("DOS-2026-00042");
        assertThat(result.typeServiceLibelle()).isEqualTo("Visa");
    }

    @Test
    void create_withExplicitDevise_shouldKeep() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(activeUsager()));
        when(encaissementRepository.nextEncaissementSequence()).thenReturn(4L);
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(inv -> {
            EncaissementEntity e = inv.getArgument(0);
            e.setId(encaissementId);
            return e;
        });

        encaissementService.create(
                new EncaissementCreateDTO(usagerId, null, new BigDecimal("50.00"), "XOF", null, null),
                caissierId,
                organisationId
        );

        ArgumentCaptor<EncaissementEntity> captor = ArgumentCaptor.forClass(EncaissementEntity.class);
        verify(encaissementRepository).save(captor.capture());
        assertThat(captor.getValue().getDevise()).isEqualTo("XOF");
    }

    @Test
    void enregistrerPaiement_whenMontantPercuNull_shouldTreatAsZero() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), null);
        UsagerEntity usager = activeUsager();

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("30.00"), ModePaiement.ESPECES, null),
                organisationId
        );

        assertThat(result.montantPercu()).isEqualByComparingTo("30.00");
        assertThat(result.statut()).isEqualTo(StatutEncaissement.PARTIEL);
    }

    @Test
    void enregistrerPaiement_withNotes_shouldSetNotes() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);
        UsagerEntity usager = activeUsager();

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("20.00"), ModePaiement.CARTE, "acompte"),
                organisationId
        );

        assertThat(result.notes()).isEqualTo("acompte");
        assertThat(enc.getNotes()).isEqualTo("acompte");
    }

    @Test
    void enregistrerPaiement_completingPartial_shouldBecomePaye() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), new BigDecimal("60.00"));
        enc.setStatut(StatutEncaissement.PARTIEL);
        UsagerEntity usager = activeUsager();

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        var result = encaissementService.enregistrerPaiement(
                encaissementId,
                new PaiementDTO(new BigDecimal("40.00"), ModePaiement.VIREMENT, null),
                organisationId
        );

        assertThat(result.statut()).isEqualTo(StatutEncaissement.PAYE);
        assertThat(result.montantPercu()).isEqualByComparingTo("100.00");
        assertThat(result.modePaiement()).isEqualTo(ModePaiement.VIREMENT);
    }

    @Test
    void list_withDossier_shouldEnrichTypeLibelle() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);
        enc.setDossierId(dossierId);
        UsagerEntity usager = activeUsager();
        DossierEntity dossier = dossier();
        TypeServiceEntity type = typeService();
        Pageable pageable = PageRequest.of(0, 20);

        when(encaissementRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(enc), pageable, 1));
        when(usagerRepository.findAllById(List.of(usagerId))).thenReturn(List.of(usager));
        when(dossierRepository.findAllById(List.of(dossierId))).thenReturn(List.of(dossier));
        when(typeServiceRepository.findAllById(List.of(typeServiceId))).thenReturn(List.of(type));

        var page = encaissementService.list(organisationId, null, null, null, null, null, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().dossierReference()).isEqualTo("DOS-2026-00042");
        assertThat(page.content().getFirst().typeServiceLibelle()).isEqualTo("Visa");
    }

    @Test
    void list_withoutDossier_shouldSkipDossierLookup() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);
        UsagerEntity usager = activeUsager();
        Pageable pageable = PageRequest.of(0, 20);

        when(encaissementRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(enc), pageable, 1));
        when(usagerRepository.findAllById(List.of(usagerId))).thenReturn(List.of(usager));

        var page = encaissementService.list(organisationId, null, null, null, null, null, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().dossierId()).isNull();
        assertThat(page.content().getFirst().typeServiceLibelle()).isNull();
        verify(dossierRepository, never()).findAllById(any());
        verify(typeServiceRepository, never()).findAllById(any());
    }

    @Test
    void getRecu_withDossierTypeCaissier_shouldPassToPdf() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);
        enc.setDossierId(dossierId);
        enc.setCaissierId(caissierId);
        UsagerEntity usager = activeUsager();
        DossierEntity dossier = dossier();
        TypeServiceEntity type = typeService();
        OrganisationEntity org = organisation();
        UserEntity caissier = caissier();
        byte[] pdf = new byte[]{9, 8, 7};

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(dossierRepository.findById(dossierId)).thenReturn(Optional.of(dossier));
        when(typeServiceRepository.findById(typeServiceId)).thenReturn(Optional.of(type));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(org));
        when(userRepository.findById(caissierId)).thenReturn(Optional.of(caissier));
        when(recuPdfService.generateRecu(enc, usager, dossier, type, org, caissier)).thenReturn(pdf);

        byte[] result = encaissementService.getRecu(encaissementId, organisationId);

        assertThat(result).isEqualTo(pdf);
        verify(recuPdfService).generateRecu(enc, usager, dossier, type, org, caissier);
    }

    @Test
    void getJournalCaisse_whenEmpty_shouldReturnZeros() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        OffsetDateTime debut = EncaissementDayBounds.startOfDay(date);
        OffsetDateTime fin = EncaissementDayBounds.startOfNextDay(date);

        when(encaissementRepository.sumByModeForDay(eq(organisationId), eq(debut), eq(fin)))
                .thenReturn(List.<Object[]>of());

        var journal = encaissementService.getJournalCaisse(date, organisationId);

        assertThat(journal.date()).isEqualTo(date);
        assertThat(journal.parMode()).isEmpty();
        assertThat(journal.nombreTransactions()).isZero();
        assertThat(journal.montantTotalPercu()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findById_shouldMapCaissierName() {
        EncaissementEntity enc = buildEncaissement(new BigDecimal("100.00"), BigDecimal.ZERO);
        enc.setCaissierId(caissierId);
        UsagerEntity usager = activeUsager();
        UserEntity caissier = caissier();

        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(enc));
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(userRepository.findById(caissierId)).thenReturn(Optional.of(caissier));

        var result = encaissementService.findById(encaissementId, organisationId);

        assertThat(result.caissierId()).isEqualTo(caissierId);
        assertThat(result.caissierNom()).isEqualTo("Fatou Diallo");
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

    private DossierEntity dossier() {
        DossierEntity dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setUsagerId(usagerId);
        dossier.setTypeServiceId(typeServiceId);
        dossier.setReference("DOS-2026-00042");
        dossier.setActif(true);
        return dossier;
    }

    private TypeServiceEntity typeService() {
        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setOrganisationId(organisationId);
        type.setLibelle("Visa");
        type.setActif(true);
        return type;
    }

    private UserEntity caissier() {
        UserEntity user = new UserEntity();
        user.setId(caissierId);
        user.setOrganisationId(organisationId);
        user.setActif(true);
        user.setNom("Diallo");
        user.setPrenom("Fatou");
        return user;
    }

    private OrganisationEntity organisation() {
        OrganisationEntity org = new OrganisationEntity();
        org.setId(organisationId);
        org.setCode("GNB");
        org.setNom("Ambassade");
        org.setPaysCode("GNB");
        org.setActif(true);
        return org;
    }

    private EncaissementEntity buildEncaissement(BigDecimal total, BigDecimal percu) {
        EncaissementEntity enc = new EncaissementEntity();
        enc.setId(encaissementId);
        enc.setOrganisationId(organisationId);
        enc.setUsagerId(usagerId);
        enc.setNumeroRecu("REC-2026-00042");
        enc.setMontantTotal(total);
        enc.setMontantPercu(percu);
        enc.setDevise("EUR");
        enc.setStatut(StatutEncaissement.DU);
        return enc;
    }
}
