package com.consulos.usager;

import com.consulos.auth.TestRedisConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class UsagerControllerIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("consulos_test")
            .withUsername("consulos")
            .withPassword("consulos_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.port", () -> 6370);
    }

    @BeforeEach
    void login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@consulos.local","password":"Admin1234!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        accessToken = body.path("data").path("accessToken").asText();
    }

    @Test
    void createUsager_shouldReturn201WithGeneratedNumero() throws Exception {
        mockMvc.perform(post("/usagers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Diallo",
                                  "prenom": "Fatou",
                                  "nationalite": "GNB",
                                  "typeUsager": "DIASPORA",
                                  "email": "fatou.diallo@email.com",
                                  "telephone": "+33698765432"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.usager.numero").isNotEmpty());
    }

    @Test
    void listUsagers_shouldReturnPaginatedList() throws Exception {
        mockMvc.perform(get("/usagers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getUsagerById_shouldReturnDetail() throws Exception {
        UUID usagerId = jdbcTemplate.queryForObject(
                "SELECT id FROM usagers WHERE numero = 'GNB-2024-00001'",
                UUID.class
        );

        mockMvc.perform(get("/usagers/" + usagerId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nom").value("Camara"));
    }

    @Test
    void searchUsagers_shouldReturnMatchingResults() throws Exception {
        mockMvc.perform(get("/usagers/search")
                        .param("q", "Camara")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nom").value("Camara"));
    }

    @Test
    void getUsagerFromOtherOrganisation_shouldReturn404() throws Exception {
        UUID senOrgId = jdbcTemplate.queryForObject(
                "SELECT id FROM organisations WHERE code = 'SEN-FR'",
                UUID.class
        );
        UUID senUsagerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO usagers (id, organisation_id, numero, nom, prenom, nationalite, type_usager, actif)
                VALUES (?, ?, 'SEN-2024-00099', 'Ndiaye', 'Amadou', 'SEN', 'DIASPORA', true)
                """, senUsagerId, senOrgId);

        mockMvc.perform(get("/usagers/" + senUsagerId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USAGER_NOT_FOUND"));

        assertThat(senOrgId).isNotNull();
    }
}
