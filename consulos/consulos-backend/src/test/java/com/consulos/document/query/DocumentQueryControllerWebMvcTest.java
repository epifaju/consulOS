package com.consulos.document.query;

import com.consulos.common.PageResponse;
import com.consulos.document.query.dto.DocumentListItemDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DocumentQueryController.class)
class DocumentQueryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentListQueryService documentListQueryService;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsEmptyPage() throws Exception {
        PageResponse<DocumentListItemDTO> page = new PageResponse<>(
                List.of(), 0, 0, 0, 20, true, true);
        when(documentListQueryService.list(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/document-queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ACCUEIL")
    void list_asAccueil_returnsForbidden() throws Exception {
        mockMvc.perform(get("/document-queries"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
