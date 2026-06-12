package com.consulos.audit;

import com.consulos.auth.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfig.class, AuditTestService.class})
@Testcontainers(disabledWithoutDocker = true)
class AuditAspectTest {

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
    private AuditTestService auditTestService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID organisationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        organisationId = jdbcTemplate.queryForObject(
                "SELECT id FROM organisations WHERE code = 'GNB-FR'", UUID.class);
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'admin@consulos.local'", UUID.class);
        MDC.put("organisationId", organisationId.toString());
        MDC.put("userId", userId.toString());
        MDC.put("requestId", UUID.randomUUID().toString());
    }

    @Test
    void auditableMethod_shouldPersistAuditLog() {
        long before = auditLogRepository.count();
        String result = auditTestService.success("data");
        assertThat(result).isEqualTo("ok-data");

        assertThat(auditLogRepository.count()).isEqualTo(before + 1);
        AuditLogEntity log = auditLogRepository.findAll().stream()
                .filter(l -> "TEST_ACTION".equals(l.getAction()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(log.getEntiteType()).isEqualTo("TEST");
        assertThat(log.getOrganisationId()).isEqualTo(organisationId);
        assertThat(log.getNouvelEtat()).contains("ok-data");
    }

    @Test
    void auditableMethod_onFailure_shouldStillPersistAuditLog() {
        long before = auditLogRepository.count();
        assertThatThrownBy(() -> auditTestService.failure()).isInstanceOf(IllegalStateException.class);
        assertThat(auditLogRepository.count()).isEqualTo(before + 1);

        AuditLogEntity log = auditLogRepository.findAll().stream()
                .filter(l -> "TEST_FAIL".equals(l.getAction()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(log.getNouvelEtat()).contains("boom");
    }

}
