package com.consulos.payment;

import java.math.BigDecimal;

/**
 * Simulateur local : URL front de confirmation (pas de Stripe requis).
 */
public class LoggingPaymentGateway implements PaymentGateway {

    private final String simulateUrlBase;

    public LoggingPaymentGateway(String simulateUrlBase) {
        this.simulateUrlBase = simulateUrlBase != null && !simulateUrlBase.isBlank()
                ? simulateUrlBase
                : "http://localhost:3000/caisse/paiement-en-ligne";
    }

    @Override
    public String providerName() {
        return "log";
    }

    @Override
    public PaymentCheckoutResult createCheckout(
            String providerSessionId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        String sep = simulateUrlBase.contains("?") ? "&" : "?";
        String url = simulateUrlBase + sep + "session=" + providerSessionId;
        return new PaymentCheckoutResult(providerSessionId, url, amount, currency);
    }
}
