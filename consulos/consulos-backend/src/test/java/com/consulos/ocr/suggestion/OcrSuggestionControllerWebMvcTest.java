package com.consulos.ocr.suggestion;

import com.consulos.ocr.dto.OcrFieldSuggestionDTO;
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

@ConsulosWebMvcTest(controllers = OcrSuggestionController.class)
@TestPropertySource(properties = "consulos.ocr.enabled=true")
class OcrSuggestionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcrSuggestionService ocrSuggestionService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void suggestions_asAgent_returnsOk() throws Exception {
        UUID documentId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        OcrFieldSuggestionDTO dto = new OcrFieldSuggestionDTO(
                documentId, "tesseract", "DONE",
                "Dupont", "Jean", "1990-01-01", "Conakry", "GN",
                "P123456", "PASSEPORT", 0.92);
        when(ocrSuggestionService.suggest(eq(documentId), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/ocr/documents/{id}/suggestions", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.data.nom").value("Dupont"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void suggestions_asAccueil_returnsForbidden() throws Exception {
        UUID documentId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

        mockMvc.perform(get("/ocr/documents/{id}/suggestions", documentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
