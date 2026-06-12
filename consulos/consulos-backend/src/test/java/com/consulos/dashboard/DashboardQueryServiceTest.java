package com.consulos.dashboard;

import com.consulos.auth.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class DashboardQueryServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("consulos_test")
            .withUsername("consulos")
            .withPassword("consulos_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.port", () -> 6370);
    }

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID organisationId;
    private UUID usagerId;
    private UUID typeServiceId;

    @BeforeEach
    void setUp() {
        organisationId = jdbcTemplate.queryForObject(
                "SELECT id FROM organisations WHERE code = 'GNB-FR'", UUID.class);
        usagerId = jdbcTemplate.queryForObject(
                "SELECT id FROM usagers WHERE numero = 'GNB-2024-00001'", UUID.class);
        typeServiceId = jdbcTemplate.queryForObject(
                "SELECT id FROM types_service WHERE code = 'PASSEPORT' LIMIT 1", UUID.class);
    }

    @Test
    void getDossierStats_shouldReturnTotals() {
        var stats = dashboardQueryService.getDossierStats(
                organisationId, LocalDate.now().minusDays(30), LocalDate.now());
        assertThat(stats.totalOuverts()).isGreaterThanOrEqualTo(0);
        assertThat(stats.delaiMoyenHeures()).isNotNull();
    }

    @Test
    void getAlertesActives_withOverdueDossier_shouldReturnEcheancesAlert() {
        jdbcTemplate.update("""
                INSERT INTO dossiers (
                    id, organisation_id, usager_id, type_service_id, reference,
                    statut, date_echeance, actif, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'DOS-TEST-00099', 'EN_REVUE', NOW() - INTERVAL '2 days', true, NOW(), NOW())
                """,
                UUID.randomUUID(), organisationId, usagerId, typeServiceId);

        var alertes = dashboardQueryService.getAlertesActives(organisationId);
        assertThat(alertes).anyMatch(a -> "ECHEANCES_DEPASSEES".equals(a.type()));
    }
}
