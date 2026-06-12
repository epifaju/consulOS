package com.consulos.security;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class SecurityIT {

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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.port", () -> 6370);
    }

    @BeforeEach
    void setUp() throws Exception {
        ensureAgentUser();
        adminToken = login("admin@consulos.local", "Admin1234!");
        agentToken = login("agent@consulos.local", "Admin1234!");
    }

    @Test
    void crossOrganisationAccess_shouldReturn404() throws Exception {
        UUID senOrgId = jdbcTemplate.queryForObject(
                "SELECT id FROM organisations WHERE code = 'SEN-FR'", UUID.class);
        UUID senUsagerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO usagers (id, organisation_id, numero, nom, prenom, nationalite, type_usager, actif)
                VALUES (?, ?, 'SEN-SEC-00001', 'Ndiaye', 'Amadou', 'SEN', 'DIASPORA', true)
                """, senUsagerId, senOrgId);

        mockMvc.perform(get("/usagers/" + senUsagerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USAGER_NOT_FOUND"));
    }

    @Test
    void adminEndpoint_withAgentRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchWithSqlInjectionAttempt_shouldNotError() throws Exception {
        mockMvc.perform(get("/usagers/search")
                        .param("q", "' OR 1=1; DROP TABLE usagers; --")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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
}
