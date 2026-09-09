package com.consulos.ocr;

import com.consulos.ocr.dto.DocumentOcrResultDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = OcrController.class)
@TestPropertySource(properties = "consulos.ocr.enabled=true")
class OcrControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcrService ocrService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void getDocumentOcr_asAgent_returnsOk() throws Exception {
        UUID documentId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        DocumentOcrResultDTO dto = new DocumentOcrResultDTO(
                documentId, "tesseract", "DONE", "extrait", null, "2024-03-15T10:00:00Z");
        when(ocrService.getResult(eq(documentId), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/ocr/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_CAISSIER")
    void getDocumentOcr_asCaissier_returnsForbidden() throws Exception {
        UUID documentId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

        mockMvc.perform(get("/ocr/documents/{id}", documentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
