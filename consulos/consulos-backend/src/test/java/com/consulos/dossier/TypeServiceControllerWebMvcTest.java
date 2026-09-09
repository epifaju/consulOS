package com.consulos.dossier;

import com.consulos.dossier.dto.TypeServiceDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = TypeServiceController.class)
class TypeServiceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TypeServiceRepository typeServiceRepository;

    @MockBean
    private TypeServiceMapper typeServiceMapper;

    @Test
    @WithConsulosUser(role = "ROLE_AGENT")
    void list_asAgent_returnsOk() throws Exception {
        TypeServiceEntity entity = new TypeServiceEntity();
        TypeServiceDTO dto = new TypeServiceDTO(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                "PASSEPORT",
                "Passeport",
                30,
                new BigDecimal("50.00"),
                null
        );
        when(typeServiceRepository.findByOrganisationIdAndActifTrue(any(UUID.class)))
                .thenReturn(List.of(entity));
        when(typeServiceMapper.toDto(entity)).thenReturn(dto);

        mockMvc.perform(get("/types-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("PASSEPORT"))
                .andExpect(jsonPath("$.data[0].libelle").value("Passeport"));
    }

    @Test
    @WithAnonymousUser
    void list_asAnonymous_returnsForbidden() throws Exception {
        mockMvc.perform(get("/types-service"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
