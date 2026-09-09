package com.consulos.rendezvous;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RdvNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private UsagerRepository usagerRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private RdvNotificationService service;

    private UUID organisationId;
    private UUID usagerId;
    private UUID typeServiceId;
    private RendezvousEntity rdv;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();

        ReflectionTestUtils.setField(service, "fromAddress", "noreply@consulos.test");

        rdv = new RendezvousEntity();
        rdv.setId(UUID.randomUUID());
        rdv.setUsagerId(usagerId);
        rdv.setOrganisationId(organisationId);
        rdv.setDateHeure(OffsetDateTime.parse("2026-09-15T10:30:00Z"));
        rdv.setStatut(RdvStatut.CONFIRME);
        rdv.setTypeServiceId(typeServiceId);
        rdv.setMotif("Demande passeport");
    }

    @Test
    void sendConfirmation_noEmail_neverSend() {
        UsagerEntity usager = usagerWithoutEmail();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        service.sendConfirmation(rdv);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendConfirmation_happy_createMimeMessageAndSend() {
        UsagerEntity usager = usagerWithEmail();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        TypeServiceEntity type = new TypeServiceEntity();
        type.setId(typeServiceId);
        type.setLibelle("Passeport");
        when(typeServiceRepository.findById(typeServiceId)).thenReturn(Optional.of(type));

        OrganisationEntity org = new OrganisationEntity();
        org.setId(organisationId);
        org.setNom("Consulat de Dakar");
        org.setPaysCode("SEN");
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(org));

        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.sendConfirmation(rdv);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mime);
    }

    @Test
    void sendAnnulation_happy() {
        UsagerEntity usager = usagerWithEmail();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.sendAnnulation(rdv, "Indisponibilité");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mime);
    }

    @Test
    void send_throws_noExceptionFromService() {
        UsagerEntity usager = usagerWithEmail();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));
        when(typeServiceRepository.findById(typeServiceId)).thenReturn(Optional.empty());
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.empty());

        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(mime);

        assertThatCode(() -> service.sendConfirmation(rdv)).doesNotThrowAnyException();
        assertThatCode(() -> service.sendAnnulation(rdv, "Motif")).doesNotThrowAnyException();
    }

    private UsagerEntity usagerWithEmail() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Traore");
        usager.setPrenom("Aminata");
        usager.setEmail("aminata@example.com");
        return usager;
    }

    private UsagerEntity usagerWithoutEmail() {
        UsagerEntity usager = new UsagerEntity();
        usager.setId(usagerId);
        usager.setOrganisationId(organisationId);
        usager.setActif(true);
        usager.setNom("Keita");
        usager.setPrenom("Moussa");
        usager.setEmail(null);
        return usager;
    }
}
