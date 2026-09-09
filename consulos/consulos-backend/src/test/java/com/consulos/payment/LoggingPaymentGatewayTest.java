package com.consulos.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingPaymentGatewayTest {

    @Test
    void providerName_shouldBeLog() {
        LoggingPaymentGateway gateway = new LoggingPaymentGateway("http://localhost:3000/pay");
        assertThat(gateway.providerName()).isEqualTo("log");
    }

    @Test
    void createCheckout_shouldBuildUrlWithSession() {
        LoggingPaymentGateway gateway = new LoggingPaymentGateway("http://localhost:3000/caisse/paiement-en-ligne");

        PaymentCheckoutResult result = gateway.createCheckout(
                "sess-123", BigDecimal.TEN, "EUR", "Test"
        );

        assertThat(result.checkoutUrl()).contains("session=sess-123");
        assertThat(result.checkoutUrl()).startsWith("http://localhost:3000/caisse/paiement-en-ligne?");
        assertThat(result.providerSessionId()).isEqualTo("sess-123");
    }

    @Test
    void createCheckout_whenBlankBase_shouldUseDefaultLocalhost() {
        LoggingPaymentGateway gateway = new LoggingPaymentGateway("  ");

        PaymentCheckoutResult result = gateway.createCheckout(
                "sess-abc", BigDecimal.ONE, "XOF", "Demo"
        );

        assertThat(result.checkoutUrl())
                .startsWith("http://localhost:3000/caisse/paiement-en-ligne?")
                .contains("session=sess-abc");
    }

    @Test
    void createCheckout_whenBaseAlreadyHasQuery_shouldUseAmpersand() {
        LoggingPaymentGateway gateway = new LoggingPaymentGateway(
                "http://localhost:3000/caisse/paiement-en-ligne?lang=fr"
        );

        PaymentCheckoutResult result = gateway.createCheckout(
                "sess-xyz", BigDecimal.valueOf(5), "EUR", "Desc"
        );

        assertThat(result.checkoutUrl())
                .isEqualTo("http://localhost:3000/caisse/paiement-en-ligne?lang=fr&session=sess-xyz");
    }
}
