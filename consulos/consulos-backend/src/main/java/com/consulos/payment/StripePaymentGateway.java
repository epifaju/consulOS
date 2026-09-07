package com.consulos.payment;

import java.math.BigDecimal;

/**
 * Stub Stripe : génère une URL de simulation tant que l'intégration SDK n'est pas branchée.
 * Remplacer par l'API Checkout Sessions Stripe quand les clés sont fournies.
 */
public class StripePaymentGateway implements PaymentGateway {

    private final PaymentProperties properties;

    public StripePaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "stripe";
    }

    @Override
    public PaymentCheckoutResult createCheckout(
            String providerSessionId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        // Sans SDK : bascule sur URL de succès configurée avec session id (démo / intégration progressive)
        String base = properties.getSuccessUrl();
        String sep = base.contains("?") ? "&" : "?";
        String url = base + sep + "session=" + providerSessionId + "&provider=stripe";
        return new PaymentCheckoutResult(providerSessionId, url, amount, currency);
    }
}
