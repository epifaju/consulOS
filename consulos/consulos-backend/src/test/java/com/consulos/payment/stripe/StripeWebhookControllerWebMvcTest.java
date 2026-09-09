package com.consulos.payment.stripe;

import com.consulos.payment.PaymentProperties;
import com.consulos.payment.PaymentService;
import com.consulos.payment.dto.PaymentSessionDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = StripeWebhookController.class)
@TestPropertySource(properties = "consulos.payment.enabled=true")
class StripeWebhookControllerWebMvcTest {

    private static final String SECRET = "whsec_test_secret";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentProperties paymentProperties;

    @BeforeEach
    void setUpWebhookSecret() {
        PaymentProperties.Stripe stripe = new PaymentProperties.Stripe();
        stripe.setWebhookSecret(SECRET);
        when(paymentProperties.getStripe()).thenReturn(stripe);
    }

    @Test
    void stripeWebhook_checkoutCompleted_completesSession() throws Exception {
        String payload = """
                {"id":"evt_1","type":"checkout.session.completed","data":{"object":{"metadata":{"consulos_session_id":"sess_abc"}}}}
                """;
        PaymentSessionDTO session = new PaymentSessionDTO(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "stripe",
                "sess_abc",
                "COMPLETED",
                new BigDecimal("25.00"),
                "EUR",
                null
        );
        when(paymentService.completeSession("sess_abc")).thenReturn(session);

        mockMvc.perform(post("/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signedHeader(payload))
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerSessionId").value("sess_abc"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(paymentService).completeSession("sess_abc");
    }

    @Test
    void stripeWebhook_ignoredEventType_returnsOkWithMessage() throws Exception {
        String payload = """
                {"id":"evt_2","type":"customer.created"}
                """;

        mockMvc.perform(post("/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signedHeader(payload))
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ignored:customer.created"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void stripeWebhook_invalidSignature_returnsBadRequest() throws Exception {
        String payload = """
                {"id":"evt_3","type":"checkout.session.completed"}
                """;
        long ts = System.currentTimeMillis() / 1000L;
        String badHeader = "t=" + ts + ",v1=deadbeef";

        mockMvc.perform(post("/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", badHeader)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("STRIPE_SIGNATURE_MISMATCH"));

        verifyNoInteractions(paymentService);
    }

    private static String signedHeader(String payload) {
        long ts = System.currentTimeMillis() / 1000L;
        return "t=" + ts + ",v1=" + hmac(SECRET, ts + "." + payload);
    }

    private static String hmac(String secret, String signedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
