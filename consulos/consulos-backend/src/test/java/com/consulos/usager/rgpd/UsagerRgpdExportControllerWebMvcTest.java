package com.consulos.usager.rgpd;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = UsagerRgpdExportController.class)
class UsagerRgpdExportControllerWebMvcTest {

    private static final UUID USAGER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsagerRgpdExportService exportService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void export_asAgent_returnsJsonAttachment() throws Exception {
        byte[] payload = "{\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8);
        when(exportService.exportJson(eq(USAGER_ID), any(UUID.class))).thenReturn(payload);
        when(exportService.suggestedFilename(eq(USAGER_ID), any(UUID.class)))
                .thenReturn("rgpd-GNB-2026-00001.json");

        mockMvc.perform(get("/usagers/{id}/export-rgpd", USAGER_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"rgpd-GNB-2026-00001.json\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().bytes(payload));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void export_asCaissier_returnsForbidden() throws Exception {
        mockMvc.perform(get("/usagers/{id}/export-rgpd", USAGER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
