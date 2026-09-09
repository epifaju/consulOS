package com.consulos.rendezvous;

import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import jakarta.mail.Session;
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
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RdvNotificationServiceRappelTest {

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
    private RendezvousEntity rdv;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        usagerId = UUID.randomUUID();

        ReflectionTestUtils.setField(service, "fromAddress", "noreply@consulos.test");

        rdv = new RendezvousEntity();
        rdv.setId(UUID.randomUUID());
        rdv.setUsagerId(usagerId);
        rdv.setOrganisationId(organisationId);
        rdv.setDateHeure(OffsetDateTime.parse("2026-09-15T10:30:00Z"));
        rdv.setStatut(RdvStatut.CONFIRME);
        rdv.setTypeServiceId(UUID.randomUUID());
        rdv.setMotif("Demande passeport");
    }

    @Test
    void sendRappel_whenNoEmail_shouldSkip() {
        UsagerEntity usager = usagerWithoutEmail();
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usager));

        service.sendRappel(rdv);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(typeServiceRepository, never()).findById(any());
    }

    @Test
    void sendRappel_whenValid_shouldSend() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usagerWithEmail()));
        when(typeServiceRepository.findById(rdv.getTypeServiceId())).thenReturn(Optional.empty());

        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.sendRappel(rdv);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mime);
    }

    @Test
    void sendAnnulation_whenMotifNull_shouldUseNonPrecise() throws Exception {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usagerWithEmail()));

        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.sendAnnulation(rdv, null);

        verify(mailSender).send(mime);
        assertThat(mime.getContent().toString()).contains("Non précisé");
    }

    @Test
    void sendConfirmation_whenUsagerMissing_shouldSkip() {
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.empty());

        service.sendConfirmation(rdv);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(typeServiceRepository, never()).findById(any());
        verify(organisationRepository, never()).findById(any());
    }

    @Test
    void sendRappel_whenTypeServiceIdNull_shouldUseServiceConsulaire() throws Exception {
        rdv.setTypeServiceId(null);
        when(usagerRepository.findById(usagerId)).thenReturn(Optional.of(usagerWithEmail()));

        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.sendRappel(rdv);

        verify(mailSender).send(mime);
        verify(typeServiceRepository, never()).findById(any());
        assertThat(mime.getContent().toString()).contains("Service consulaire");
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
