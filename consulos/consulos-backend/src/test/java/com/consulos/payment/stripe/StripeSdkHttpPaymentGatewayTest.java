package com.consulos.payment.stripe;

import com.consulos.common.BusinessException;
import com.consulos.payment.PaymentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeSdkHttpPaymentGatewayTest {

    @Test
    void createCheckout_whenSecretMissing_shouldThrowNotConfigured() {
        PaymentProperties properties = new PaymentProperties();
        properties.getStripe().setSecretKey(" ");
        StripeSdkHttpPaymentGateway gateway =
                new StripeSdkHttpPaymentGateway(properties, RestClient.builder());

        assertThatThrownBy(() -> gateway.createCheckout(
                "sess-1", new BigDecimal("25.50"), "EUR", "Passeport"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("STRIPE_NOT_CONFIGURED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void providerName_shouldBeStripeSdk() {
        StripeSdkHttpPaymentGateway gateway =
                new StripeSdkHttpPaymentGateway(new PaymentProperties(), RestClient.builder());
        assertThat(gateway.providerName()).isEqualTo("stripe-sdk");
    }
}
