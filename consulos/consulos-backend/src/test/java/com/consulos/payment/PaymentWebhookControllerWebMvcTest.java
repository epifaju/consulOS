package com.consulos.payment;

import com.consulos.payment.dto.PaymentSessionDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = PaymentWebhookController.class)
@TestPropertySource(properties = "consulos.payment.enabled=true")
class PaymentWebhookControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void logWebhook_withSessionId_returnsOk() throws Exception {
        PaymentSessionDTO session = new PaymentSessionDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "log",
                "pay_1",
                "COMPLETED",
                new BigDecimal("25.00"),
                "EUR",
                null
        );
        when(paymentService.completeSession(eq("pay_1"))).thenReturn(session);

        mockMvc.perform(post("/payments/webhook/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"pay_1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerSessionId").value("pay_1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
