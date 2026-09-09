package com.consulos.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StripePaymentGatewayTest {

    @Test
    void createCheckout_shouldContainSessionAndProviderStripe() {
        PaymentProperties properties = new PaymentProperties();
        properties.setSuccessUrl("http://localhost:3000/caisse?payment=success");

        StripePaymentGateway gateway = new StripePaymentGateway(properties);

        PaymentCheckoutResult result = gateway.createCheckout(
                "cs_test_123", BigDecimal.valueOf(25), "EUR", "Paiement dossier"
        );

        assertThat(gateway.providerName()).isEqualTo("stripe");
        assertThat(result.checkoutUrl()).contains("session=cs_test_123");
        assertThat(result.checkoutUrl()).contains("provider=stripe");
        assertThat(result.checkoutUrl()).startsWith("http://localhost:3000/caisse?payment=success&");
    }
}
