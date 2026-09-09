package com.consulos.audit;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.audit.dto.AuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditService auditService;

    private UUID organisationId;
    private UUID userId;
    private UUID logId;
    private AuditLogEntity logEntity;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        logId = UUID.randomUUID();

        logEntity = new AuditLogEntity();
        logEntity.setId(logId);
        logEntity.setOrganisationId(organisationId);
        logEntity.setUserId(userId);
        logEntity.setAction("DOSSIER_TRANSITION");
        logEntity.setEntiteType("DOSSIER");
        logEntity.setEntiteId(UUID.randomUUID());
        logEntity.setIpAddress("10.0.0.8");
        logEntity.setUserAgent("JUnit");
        logEntity.setCreatedAt(OffsetDateTime.parse("2026-09-09T10:15:00Z"));

        user = new UserEntity();
        user.setId(userId);
        user.setNom("Camara");
        user.setPrenom("Aminata");
    }

    @Test
    void list_shouldMapLogsWithUserNamesAndOrganisationScope() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logEntity), PageRequest.of(0, 20), 1));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        var page = auditService.list(
                organisationId,
                "DOSSIER_TRANSITION",
                "DOSSIER",
                userId,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(page.content()).hasSize(1);
        AuditLogDTO dto = page.content().getFirst();
        assertThat(dto.id()).isEqualTo(logId);
        assertThat(dto.organisationId()).isEqualTo(organisationId);
        assertThat(dto.userNom()).isEqualTo("Camara");
        assertThat(dto.userPrenom()).isEqualTo("Aminata");
        assertThat(dto.action()).isEqualTo("DOSSIER_TRANSITION");
        assertThat(dto.ipAddress()).isEqualTo("10.0.0.8");
    }

    @Test
    void list_whenNoUserIds_shouldSkipUserLookup() {
        logEntity.setUserId(null);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logEntity)));

        var page = auditService.list(organisationId, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.content().getFirst().userNom()).isNull();
        assertThat(page.content().getFirst().userPrenom()).isNull();
    }

    @Test
    void exportCsv_shouldEscapeValuesAndIncludeHeader() {
        when(auditLogRepository.findAll(any(Specification.class), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(logEntity)));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        String csv = new String(
                auditService.exportCsv(organisationId, null, null, null, null, null),
                StandardCharsets.UTF_8
        );

        assertThat(csv).startsWith("date,utilisateur,action,entite_type,entite_id,ip_address\n");
        assertThat(csv).contains("Aminata Camara");
        assertThat(csv).contains("DOSSIER_TRANSITION");
        assertThat(csv).contains("10.0.0.8");
    }

    @Test
    void exportCsv_shouldEscapeCommasInUserAgentFieldsViaAction() {
        logEntity.setAction("ACTION,WITH,COMMA");
        when(auditLogRepository.findAll(any(Specification.class), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(logEntity)));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        String csv = new String(
                auditService.exportCsv(organisationId, null, null, null, null, null),
                StandardCharsets.UTF_8
        );

        assertThat(csv).contains("\"ACTION,WITH,COMMA\"");
    }
}
