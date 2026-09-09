package com.consulos.payment;

import com.consulos.payment.dto.PaymentSessionDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = PaymentController.class)
@TestPropertySource(properties = "consulos.payment.enabled=true")
class PaymentControllerWebMvcTest {

    private static final UUID ENCAISSEMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String PROVIDER_SESSION_ID = "ps_test_1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentProperties properties;

    @MockBean
    private PaymentGateway paymentGateway;

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void status_asCaissier_returnsOk() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(paymentGateway.providerName()).thenReturn("log");

        mockMvc.perform(get("/payments/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.provider").value("log"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void checkout_asCaissier_returnsOk() throws Exception {
        PaymentSessionDTO session = sampleSession();
        when(paymentService.createCheckout(eq(ENCAISSEMENT_ID), any(UUID.class))).thenReturn(session);

        mockMvc.perform(post("/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encaissementId":"%s"}
                                """.formatted(ENCAISSEMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerSessionId").value(PROVIDER_SESSION_ID))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void checkout_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(post("/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"encaissementId":"%s"}
                                """.formatted(ENCAISSEMENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void getSession_asCaissier_returnsOk() throws Exception {
        when(paymentService.getSession(PROVIDER_SESSION_ID)).thenReturn(sampleSession());

        mockMvc.perform(get("/payments/sessions/{id}", PROVIDER_SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerSessionId").value(PROVIDER_SESSION_ID));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void complete_asCaissier_returnsOk() throws Exception {
        PaymentSessionDTO completed = new PaymentSessionDTO(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                ENCAISSEMENT_ID,
                "log",
                PROVIDER_SESSION_ID,
                "COMPLETED",
                new BigDecimal("25.00"),
                "EUR",
                null
        );
        when(paymentService.completeSession(PROVIDER_SESSION_ID)).thenReturn(completed);

        mockMvc.perform(post("/payments/sessions/{id}/complete", PROVIDER_SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    private static PaymentSessionDTO sampleSession() {
        return new PaymentSessionDTO(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                ENCAISSEMENT_ID,
                "log",
                PROVIDER_SESSION_ID,
                "PENDING",
                new BigDecimal("25.00"),
                "EUR",
                "http://localhost/checkout"
        );
    }
}
