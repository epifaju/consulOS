package com.consulos.payment;

import com.consulos.common.BusinessException;
import com.consulos.encaissement.EncaissementEntity;
import com.consulos.encaissement.EncaissementRepository;
import com.consulos.encaissement.EncaissementService;
import com.consulos.encaissement.ModePaiement;
import com.consulos.encaissement.StatutEncaissement;
import com.consulos.encaissement.dto.PaiementDTO;
import com.consulos.payment.dto.PaymentSessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
public class PaymentService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final PaymentSessionRepository sessionRepository;
    private final EncaissementRepository encaissementRepository;
    private final EncaissementService encaissementService;
    private final PaymentGateway paymentGateway;
    private final PaymentProperties properties;

    @Transactional
    public PaymentSessionDTO createCheckout(UUID encaissementId, UUID organisationId) {
        EncaissementEntity enc = encaissementRepository
                .findByIdAndOrganisationId(encaissementId, organisationId)
                .orElseThrow(() -> new BusinessException(
                        "ENCAISSEMENT_NOT_FOUND", "Encaissement introuvable", HttpStatus.NOT_FOUND));

        if (enc.getStatut() == StatutEncaissement.PAYE
                || enc.getStatut() == StatutEncaissement.EXONERE
                || enc.getStatut() == StatutEncaissement.REMBOURSE) {
            throw new BusinessException(
                    "PAYMENT_NOT_ALLOWED", "Cet encaissement n'est pas payable en ligne", HttpStatus.BAD_REQUEST);
        }

        BigDecimal percu = enc.getMontantPercu() != null ? enc.getMontantPercu() : BigDecimal.ZERO;
        BigDecimal reste = enc.getMontantTotal().subtract(percu);
        if (reste.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("NOTHING_DUE", "Aucun montant restant dû", HttpStatus.BAD_REQUEST);
        }

        String providerSessionId = "pay_" + UUID.randomUUID().toString().replace("-", "");
        PaymentCheckoutResult checkout = paymentGateway.createCheckout(
                providerSessionId,
                reste,
                enc.getDevise() != null ? enc.getDevise() : "EUR",
                "ConsulOS " + enc.getNumeroRecu()
        );

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        PaymentSessionEntity session = new PaymentSessionEntity();
        session.setId(UUID.randomUUID());
        session.setOrganisationId(organisationId);
        session.setEncaissementId(encaissementId);
        session.setProvider(paymentGateway.providerName());
        session.setProviderSessionId(checkout.providerSessionId());
        session.setStatus(STATUS_PENDING);
        session.setAmount(reste);
        session.setCurrency(checkout.currency());
        session.setCheckoutUrl(checkout.checkoutUrl());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        log.info("Checkout {} créé pour encaissement {} ({})", session.getProviderSessionId(), enc.getNumeroRecu(), reste);
        return toDto(session);
    }

    @Transactional(readOnly = true)
    public PaymentSessionDTO getSession(String providerSessionId) {
        return sessionRepository.findByProviderSessionId(providerSessionId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(
                        "PAYMENT_SESSION_NOT_FOUND", "Session de paiement introuvable", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public PaymentSessionDTO completeSession(String providerSessionId) {
        PaymentSessionEntity session = sessionRepository.findByProviderSessionId(providerSessionId)
                .orElseThrow(() -> new BusinessException(
                        "PAYMENT_SESSION_NOT_FOUND", "Session de paiement introuvable", HttpStatus.NOT_FOUND));

        if (STATUS_COMPLETED.equals(session.getStatus())) {
            return toDto(session);
        }
        if (!STATUS_PENDING.equals(session.getStatus())) {
            throw new BusinessException(
                    "PAYMENT_SESSION_INVALID", "Session non payable", HttpStatus.BAD_REQUEST);
        }

        // Mode EXTERNE = paiement hors caisse (en ligne) sans modifier l'enum ModePaiement
        encaissementService.enregistrerPaiement(
                session.getEncaissementId(),
                new PaiementDTO(session.getAmount(), ModePaiement.EXTERNE, "Paiement en ligne (" + session.getProvider() + ")"),
                session.getOrganisationId()
        );

        session.setStatus(STATUS_COMPLETED);
        session.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        session.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        sessionRepository.save(session);
        return toDto(session);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private PaymentSessionDTO toDto(PaymentSessionEntity session) {
        return new PaymentSessionDTO(
                session.getId(),
                session.getEncaissementId(),
                session.getProvider(),
                session.getProviderSessionId(),
                session.getStatus(),
                session.getAmount(),
                session.getCurrency(),
                session.getCheckoutUrl()
        );
    }
}
