package com.consulos.rendezvous;

import com.consulos.rendezvous.dto.ConfigCreneauDTO;
import com.consulos.testsupport.ConsulosWebMvcTest;
import com.consulos.testsupport.WithConsulosUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConsulosWebMvcTest(controllers = ParametresController.class)
class ParametresControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParametresService parametresService;

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void listCreneaux_asSuperviseur_returnsOk() throws Exception {
        when(parametresService.listCreneaux(any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/parametres/creneaux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithConsulosUser(role = "ROLE_ADMIN")
    void createCreneau_asAdmin_returnsCreated() throws Exception {
        ConfigCreneauDTO created = new ConfigCreneauDTO(
                UUID.randomUUID(), null, 1,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                30, 5, true
        );
        when(parametresService.createCreneau(any(ConfigCreneauDTO.class), any(UUID.class)))
                .thenReturn(created);

        mockMvc.perform(post("/parametres/creneaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":null,
                                  "typeServiceId":null,
                                  "jourSemaine":1,
                                  "heureDebut":"09:00:00",
                                  "heureFin":"12:00:00",
                                  "dureeMinutes":30,
                                  "quotaMax":5,
                                  "actif":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jourSemaine").value(1));
    }

    @Test
    @WithConsulosUser(role = "ROLE_SUPERVISEUR")
    void createCreneau_asSuperviseur_returnsForbidden() throws Exception {
        mockMvc.perform(post("/parametres/creneaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jourSemaine":1,
                                  "heureDebut":"09:00:00",
                                  "heureFin":"12:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
