package com.consulos.document.storage;

import com.consulos.document.storage.dto.DocumentStorageReconciliationDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = DocumentStorageReconciliationController.class)
class DocumentStorageReconciliationControllerWebMvcTest {

    private static final UUID ORG_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentStorageReconciliationService reconciliationService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void reconcile_asAdmin_returnsOk() throws Exception {
        when(reconciliationService.reconcile(ORG_ID)).thenReturn(
                new DocumentStorageReconciliationDTO(3, 2, 1, 0, List.of(), List.of()));

        mockMvc.perform(get("/admin/document-storage/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActifs").value(3))
                .andExpect(jsonPath("$.data.absentsDeMinio").value(1));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void reconcile_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/document-storage/reconciliation"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        verifyNoInteractions(reconciliationService);
    }
}
