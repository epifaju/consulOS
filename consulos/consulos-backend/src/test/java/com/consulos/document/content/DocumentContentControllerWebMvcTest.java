package com.consulos.document.content;

import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@ConsulosWebMvcTest(controllers = DocumentContentController.class)
class DocumentContentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentContentService documentContentService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void content_asAgent_returnsBytesWithHeaders() throws Exception {
        UUID id = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        byte[] bytes = "pdf-bytes".getBytes(StandardCharsets.UTF_8);
        when(documentContentService.open(eq(id), any(UUID.class)))
                .thenReturn(new DocumentContentPayload(bytes, "doc.pdf", "application/pdf"));

        mockMvc.perform(get("/documents/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Length", String.valueOf(bytes.length)))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void content_asAccueil_returnsForbidden() throws Exception {
        UUID id = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

        mockMvc.perform(get("/documents/{id}/content", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
