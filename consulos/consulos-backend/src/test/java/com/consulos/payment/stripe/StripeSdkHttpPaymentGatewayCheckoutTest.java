package com.consulos.payment.stripe;

import com.consulos.payment.PaymentCheckoutResult;
import com.consulos.payment.PaymentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StripeSdkHttpPaymentGatewayCheckoutTest {

    private PaymentProperties properties;
    private MockRestServiceServer mockServer;
    private StripeSdkHttpPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.getStripe().setSecretKey("sk_test_secret");
        properties.setSuccessUrl("http://localhost:3010/caisse/success");
        properties.setCancelUrl("http://localhost:3010/caisse/cancel");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        gateway = new StripeSdkHttpPaymentGateway(properties, builder);
    }

    @Test
    void createCheckout_whenCurrencyNull_defaultsEur() {
        mockServer.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("unit_amount")))
                .andExpect(content().string(containsString("2550")))
                .andExpect(content().string(containsString("eur")))
                .andRespond(withSuccess(
                        "{\"id\":\"cs_test\",\"url\":\"https://checkout.stripe.com/c/pay/cs_test\"}",
                        MediaType.APPLICATION_JSON
                ));

        PaymentCheckoutResult result = gateway.createCheckout(
                "sess-1", new BigDecimal("25.50"), null, "Passeport");

        mockServer.verify();
        assertThat(result.providerSessionId()).isEqualTo("sess-1");
        assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test");
        assertThat(result.amount()).isEqualByComparingTo("25.50");
        assertThat(result.currency()).isEqualTo("EUR");
    }
}
