package com.consulos.document;

import com.consulos.document.dto.DocumentDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DocumentController.class)
class DocumentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void getById_asAgent_returnsOk() throws Exception {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        DocumentDTO dto = new DocumentDTO(
                id, null, null, "file.pdf", "file.pdf", "PASSEPORT",
                "application/pdf", 100L, null, 1, null, null
        );
        when(documentService.findById(eq(id), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.nomFichier").value("file.pdf"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void delete_asAdmin_returnsNoContent() throws Exception {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        doNothing().when(documentService).softDelete(eq(id), any(UUID.class));

        mockMvc.perform(delete("/documents/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void delete_asAgent_returnsForbidden() throws Exception {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        mockMvc.perform(delete("/documents/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
