package com.consulos.audit;

import com.consulos.audit.dto.AuditLogDTO;
import com.consulos.common.PageResponse;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = AuditController.class)
class AuditControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void listLogs_asAdmin_returnsOkPage() throws Exception {
        PageResponse<AuditLogDTO> page = new PageResponse<>(
                List.of(), 0, 0, 0, 50, true, true);
        when(auditService.list(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void listLogs_asAgent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void export_asAdmin_returnsCsvBytes() throws Exception {
        byte[] csv = "action,entite\nLOGIN,USER\n".getBytes(StandardCharsets.UTF_8);
        when(auditService.exportCsv(
                any(UUID.class),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(csv);

        mockMvc.perform(get("/audit/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit-logs.csv\""))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().bytes(csv));
    }
}
