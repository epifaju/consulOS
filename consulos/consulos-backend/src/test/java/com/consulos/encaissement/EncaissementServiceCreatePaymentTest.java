package com.consulos.encaissement;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.auth.repository.UserRepository;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.encaissement.dto.EncaissementCreateDTO;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncaissementServiceCreatePaymentTest {

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

    @Test
    void create_withEspeces_recordsImmediatePayment() {
        UUID organisationId = UUID.randomUUID();
        UUID usagerId = UUID.randomUUID();
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Camara");
        usager.setPrenom("Mamadou");
        usager.setNumero("USG-010");

        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(encaissementRepository.nextEncaissementSequence()).thenReturn(8L);
        when(encaissementRepository.save(any(EncaissementEntity.class))).thenAnswer(invocation -> {
            EncaissementEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        encaissementService.create(
                new EncaissementCreateDTO(
                        usagerId, null, new BigDecimal("50.00"), "EUR", ModePaiement.ESPECES, null),
                UUID.randomUUID(),
                organisationId
        );

        ArgumentCaptor<EncaissementEntity> captor = ArgumentCaptor.forClass(EncaissementEntity.class);
        verify(encaissementRepository).save(captor.capture());
        EncaissementEntity saved = captor.getValue();

        assertThat(saved.getStatut()).isEqualTo(StatutEncaissement.PAYE);
        assertThat(saved.getMontantPercu()).isEqualByComparingTo("50.00");
        assertThat(saved.getModePaiement()).isEqualTo(ModePaiement.ESPECES);
        assertThat(saved.getDatePaiement()).isNotNull();
    }
}
