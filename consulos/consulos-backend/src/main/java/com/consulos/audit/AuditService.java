package com.consulos.audit;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.audit.dto.AuditLogDTO;
import com.consulos.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> list(
            UUID organisationId,
            String action,
            String entiteType,
            UUID userId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            Pageable pageable
    ) {
        Specification<AuditLogEntity> spec = Specification.where(AuditSpecifications.forOrganisation(organisationId))
                .and(AuditSpecifications.withAction(action))
                .and(AuditSpecifications.withEntiteType(entiteType))
                .and(AuditSpecifications.withUserId(userId))
                .and(AuditSpecifications.betweenDates(dateDebut, dateFin));

        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, pageable);
        Map<UUID, UserEntity> users = loadUsers(page.getContent());

        List<AuditLogDTO> content = page.getContent().stream()
                .map(log -> toDto(log, users))
                .toList();

        return new PageResponse<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(
            UUID organisationId,
            String action,
            String entiteType,
            UUID userId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin
    ) {
        PageResponse<AuditLogDTO> page = list(
                organisationId, action, entiteType, userId, dateDebut, dateFin, Pageable.unpaged()
        );
        StringBuilder csv = new StringBuilder();
        csv.append("date,utilisateur,action,entite_type,entite_id,ip_address\n");
        for (AuditLogDTO log : page.content()) {
            String user = (log.userPrenom() != null ? log.userPrenom() : "")
                    + " " + (log.userNom() != null ? log.userNom() : "");
            csv.append(escapeCsv(log.createdAt() != null ? log.createdAt().toString() : "")).append(',');
            csv.append(escapeCsv(user.trim())).append(',');
            csv.append(escapeCsv(log.action())).append(',');
            csv.append(escapeCsv(log.entiteType())).append(',');
            csv.append(escapeCsv(log.entiteId() != null ? log.entiteId().toString() : "")).append(',');
            csv.append(escapeCsv(log.ipAddress())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<UUID, UserEntity> loadUsers(List<AuditLogEntity> logs) {
        List<UUID> ids = logs.stream()
                .map(AuditLogEntity::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private AuditLogDTO toDto(AuditLogEntity entity, Map<UUID, UserEntity> users) {
        UserEntity user = entity.getUserId() != null ? users.get(entity.getUserId()) : null;
        return new AuditLogDTO(
                entity.getId(),
                entity.getOrganisationId(),
                entity.getUserId(),
                user != null ? user.getNom() : null,
                user != null ? user.getPrenom() : null,
                entity.getAction(),
                entity.getEntiteType(),
                entity.getEntiteId(),
                entity.getAncienEtat(),
                entity.getNouvelEtat(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getCreatedAt()
        );
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
