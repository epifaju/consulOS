package com.consulos.payment;

import com.consulos.common.BusinessException;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.encaissement.EncaissementRepository;
import com.consulos.encaissement.EncaissementService;
import com.consulos.encaissement.ModePaiement;
import com.consulos.encaissement.StatutEncaissement;
import com.consulos.encaissement.dto.PaiementDTO;
import com.consulos.payment.dto.PaymentSessionDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentSessionRepository sessionRepository;
    @Mock
    private EncaissementRepository encaissementRepository;
    @Mock
    private EncaissementService encaissementService;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentProperties properties;

    @InjectMocks
    private PaymentService paymentService;

    private UUID organisationId;
    private UUID encaissementId;
    private EncaissementEntity encaissement;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        encaissementId = UUID.randomUUID();

        encaissement = new EncaissementEntity();
        encaissement.setId(encaissementId);
        encaissement.setOrganisationId(organisationId);
        encaissement.setNumeroRecu("REC-2026-00010");
        encaissement.setMontantTotal(new BigDecimal("100.00"));
        encaissement.setMontantPercu(new BigDecimal("40.00"));
        encaissement.setDevise("EUR");
        encaissement.setStatut(StatutEncaissement.PARTIEL);
    }

    @Test
    void createCheckout_shouldPersistPendingSessionForRemainingAmount() {
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(encaissement));
        when(paymentGateway.providerName()).thenReturn("log");
        when(paymentGateway.createCheckout(any(), eq(new BigDecimal("60.00")), eq("EUR"), any()))
                .thenAnswer(invocation -> new PaymentCheckoutResult(
                        invocation.getArgument(0),
                        "https://pay.local/checkout",
                        invocation.getArgument(1),
                        "EUR"
                ));
        when(sessionRepository.save(any(PaymentSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentSessionDTO result = paymentService.createCheckout(encaissementId, organisationId);

        ArgumentCaptor<PaymentSessionEntity> captor = ArgumentCaptor.forClass(PaymentSessionEntity.class);
        verify(sessionRepository).save(captor.capture());
        PaymentSessionEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentService.STATUS_PENDING);
        assertThat(saved.getAmount()).isEqualByComparingTo("60.00");
        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getEncaissementId()).isEqualTo(encaissementId);
        assertThat(saved.getProvider()).isEqualTo("log");
        assertThat(result.checkoutUrl()).isEqualTo("https://pay.local/checkout");
        assertThat(result.amount()).isEqualByComparingTo("60.00");
    }

    @Test
    void createCheckout_whenPaye_shouldReject() {
        encaissement.setStatut(StatutEncaissement.PAYE);
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(encaissement));

        assertThatThrownBy(() -> paymentService.createCheckout(encaissementId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("PAYMENT_NOT_ALLOWED"));
    }

    @Test
    void createCheckout_whenExonere_shouldReject() {
        encaissement.setStatut(StatutEncaissement.EXONERE);
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(encaissement));

        assertThatThrownBy(() -> paymentService.createCheckout(encaissementId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("PAYMENT_NOT_ALLOWED"));
    }

    @Test
    void createCheckout_whenNothingDue_shouldReject() {
        encaissement.setMontantPercu(new BigDecimal("100.00"));
        encaissement.setStatut(StatutEncaissement.DU);
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.of(encaissement));

        assertThatThrownBy(() -> paymentService.createCheckout(encaissementId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("NOTHING_DUE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void createCheckout_whenOtherOrganisation_shouldThrowNotFound() {
        when(encaissementRepository.findByIdAndOrganisationId(encaissementId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createCheckout(encaissementId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("ENCAISSEMENT_NOT_FOUND"));
    }

    @Test
    void completeSession_shouldRegisterExternePaymentAndMarkCompleted() {
        PaymentSessionEntity session = pendingSession();
        when(sessionRepository.findByProviderSessionId("pay_abc")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(PaymentSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentSessionDTO result = paymentService.completeSession("pay_abc");

        ArgumentCaptor<PaiementDTO> paiementCaptor = ArgumentCaptor.forClass(PaiementDTO.class);
        verify(encaissementService).enregistrerPaiement(
                eq(encaissementId),
                paiementCaptor.capture(),
                eq(organisationId)
        );
        assertThat(paiementCaptor.getValue().modePaiement()).isEqualTo(ModePaiement.EXTERNE);
        assertThat(paiementCaptor.getValue().montant()).isEqualByComparingTo("60.00");
        assertThat(session.getStatus()).isEqualTo(PaymentService.STATUS_COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(result.status()).isEqualTo(PaymentService.STATUS_COMPLETED);
    }

    @Test
    void completeSession_whenAlreadyCompleted_shouldBeIdempotent() {
        PaymentSessionEntity session = pendingSession();
        session.setStatus(PaymentService.STATUS_COMPLETED);
        when(sessionRepository.findByProviderSessionId("pay_abc")).thenReturn(Optional.of(session));

        PaymentSessionDTO result = paymentService.completeSession("pay_abc");

        assertThat(result.status()).isEqualTo(PaymentService.STATUS_COMPLETED);
        verify(encaissementService, never()).enregistrerPaiement(any(), any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void completeSession_whenCancelled_shouldReject() {
        PaymentSessionEntity session = pendingSession();
        session.setStatus(PaymentService.STATUS_CANCELLED);
        when(sessionRepository.findByProviderSessionId("pay_abc")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> paymentService.completeSession("pay_abc"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("PAYMENT_SESSION_INVALID"));
    }

    @Test
    void getSession_whenMissing_shouldThrowNotFound() {
        when(sessionRepository.findByProviderSessionId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getSession("missing"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo("PAYMENT_SESSION_NOT_FOUND"));
    }

    private PaymentSessionEntity pendingSession() {
        PaymentSessionEntity session = new PaymentSessionEntity();
        session.setId(UUID.randomUUID());
        session.setOrganisationId(organisationId);
        session.setEncaissementId(encaissementId);
        session.setProvider("log");
        session.setProviderSessionId("pay_abc");
        session.setStatus(PaymentService.STATUS_PENDING);
        session.setAmount(new BigDecimal("60.00"));
        session.setCurrency("EUR");
        session.setCheckoutUrl("https://pay.local/checkout");
        return session;
    }
}
