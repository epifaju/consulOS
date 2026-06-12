package com.consulos.dossier;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class DossierControllerIT {

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

    private String adminToken;
    private String agentToken;
    private UUID usagerId;
    private UUID typeServiceId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.port", () -> 6370);
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin@consulos.local", "Admin1234!");
        ensureAgentUser();
        agentToken = login("agent@consulos.local", "Admin1234!");

        usagerId = jdbcTemplate.queryForObject(
                "SELECT id FROM usagers WHERE numero = 'GNB-2024-00001'",
                UUID.class
        );
        typeServiceId = jdbcTemplate.queryForObject(
                "SELECT id FROM types_service WHERE code = 'PASSEPORT' LIMIT 1",
                UUID.class
        );
    }

    @Test
    void createDossier_shouldReturn201WithReference() throws Exception {
        mockMvc.perform(post("/dossiers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usagerId": "%s",
                                  "typeServiceId": "%s"
                                }
                                """.formatted(usagerId, typeServiceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").isNotEmpty());
    }

    @Test
    void transitionBrouillonToSoumis_shouldReturn200() throws Exception {
        String dossierId = createDossier();

        mockMvc.perform(post("/dossiers/" + dossierId + "/transition")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statut": "SOUMIS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("SOUMIS"));
    }

    @Test
    void transitionEnRevueToValideWithAgent_shouldReturn403() throws Exception {
        String dossierId = createDossier();
        transition(dossierId, adminToken, "SOUMIS");
        transition(dossierId, adminToken, "EN_REVUE");

        mockMvc.perform(post("/dossiers/" + dossierId + "/transition")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statut": "VALIDE"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ROLE_INSUFFISANT"));
    }

    private void ensureAgentUser() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'agent@consulos.local'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO users (organisation_id, email, password_hash, nom, prenom, role_id)
                SELECT o.id,
                       'agent@consulos.local',
                       '$2b$12$BXEkQ5aHPdeGaGSb.ScvtOUl5iwZ6iB1a45XZTHHPncr79VEG5Yxm',
                       'Agent',
                       'Test',
                       r.id
                FROM organisations o, roles r
                WHERE o.code = 'GNB-FR' AND r.code = 'ROLE_AGENT'
                """);
    }

    private String login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    private String createDossier() throws Exception {
        MvcResult result = mockMvc.perform(post("/dossiers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usagerId": "%s",
                                  "typeServiceId": "%s"
                                }
                                """.formatted(usagerId, typeServiceId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String reference = body.path("data").path("reference").asText();
        assertThat(reference).isNotBlank();
        return body.path("data").path("id").asText();
    }

    private void transition(String dossierId, String token, String statut) throws Exception {
        mockMvc.perform(post("/dossiers/" + dossierId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statut": "%s"}
                                """.formatted(statut)))
                .andExpect(status().isOk());
    }
}
