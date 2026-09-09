package com.consulos.sms;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = SmsStatusController.class)
@TestPropertySource(properties = "consulos.sms.enabled=true")
class SmsStatusControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmsProperties properties;

    @MockBean
    private SmsGateway smsGateway;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void status_asAdmin_returnsOk() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getFromNumber()).thenReturn("+33123456789");
        when(smsGateway.providerName()).thenReturn("log");

        mockMvc.perform(get("/sms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.provider").value("log"))
                .andExpect(jsonPath("$.data.fromNumberConfigured").value(true));
    }

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void status_asSuperviseur_returnsOk() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getFromNumber()).thenReturn("+33123456789");
        when(smsGateway.providerName()).thenReturn("log");

        mockMvc.perform(get("/sms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void status_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/sms/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
