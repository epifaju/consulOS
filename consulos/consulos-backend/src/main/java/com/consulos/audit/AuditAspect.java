package com.consulos.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(com.consulos.audit.Auditable)"
            + " || @annotation(com.consulos.dossier.Auditable)"
            + " || @annotation(com.consulos.document.Auditable)"
            + " || @annotation(com.consulos.encaissement.Auditable)"
            + " || @annotation(com.consulos.rendezvous.Auditable)")
    public void auditableMethods() {
    }

    @Around("auditableMethods()")
    public Object aroundAuditable(ProceedingJoinPoint joinPoint) throws Throwable {
        AuditMetadata metadata = resolveMetadata(joinPoint).orElse(null);
        String ancienEtat = safeSerialize(buildParamsSnapshot(joinPoint));

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            if (metadata != null) {
                persistAuditLog(joinPoint, metadata, ancienEtat, result, error);
            }
        }
    }

    private void persistAuditLog(
            ProceedingJoinPoint joinPoint,
            AuditMetadata metadata,
            String ancienEtat,
            Object result,
            Throwable error
    ) {
        try {
            AuditLogEntity logEntity = new AuditLogEntity();
            logEntity.setOrganisationId(parseUuid(MDC.get("organisationId")));
            logEntity.setUserId(parseUuid(MDC.get("userId")));
            logEntity.setAction(metadata.action());
            logEntity.setEntiteType(resolveEntiteType(metadata));
            logEntity.setEntiteId(extractEntityId(joinPoint.getArgs(), result));
            logEntity.setAncienEtat(ancienEtat);
            logEntity.setNouvelEtat(buildNouvelEtat(result, error));
            logEntity.setIpAddress(resolveClientIp());
            logEntity.setUserAgent(resolveUserAgent());
            logEntity.setCreatedAt(OffsetDateTime.now());

            if (logEntity.getUserId() == null) {
                logEntity.setUserId(resolveUserIdFromSecurityContext());
            }

            auditLogRepository.save(logEntity);
        } catch (Exception ex) {
            log.warn("Échec persistance audit pour action={}: {}", metadata.action(), ex.getMessage());
        }
    }

    private String buildNouvelEtat(Object result, Throwable error) {
        if (error != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("success", false);
            payload.put("errorType", error.getClass().getSimpleName());
            payload.put("errorMessage", error.getMessage());
            return safeSerialize(payload);
        }
        return safeSerialize(result);
    }

    private Map<String, Object> buildParamsSnapshot(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> params = new HashMap<>();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                params.put(names[i], sanitize(args[i]));
            }
        }
        return params;
    }

    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        String typeName = value.getClass().getSimpleName().toLowerCase();
        if (typeName.contains("password") || typeName.contains("multipart")) {
            return "[REDACTED]";
        }
        return value;
    }

    private Optional<AuditMetadata> resolveMetadata(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        for (Annotation annotation : method.getAnnotations()) {
            if (!"Auditable".equals(annotation.annotationType().getSimpleName())) {
                continue;
            }
            try {
                String action = (String) annotation.annotationType().getMethod("action").invoke(annotation);
                String entiteType = "";
                try {
                    entiteType = (String) annotation.annotationType().getMethod("entiteType").invoke(annotation);
                } catch (NoSuchMethodException ignored) {
                    // annotations métier sans entiteType
                }
                if (action == null || action.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(new AuditMetadata(action, entiteType));
            } catch (ReflectiveOperationException ex) {
                log.warn("Impossible de lire @Auditable: {}", ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private String resolveEntiteType(AuditMetadata metadata) {
        if (metadata.entiteType() != null && !metadata.entiteType().isBlank()) {
            return metadata.entiteType();
        }
        String action = metadata.action();
        if (action.contains("DOSSIER")) {
            return "DOSSIER";
        }
        if (action.contains("DOCUMENT")) {
            return "DOCUMENT";
        }
        if (action.contains("ENCAISSEMENT") || action.contains("PAIEMENT")) {
            return "ENCAISSEMENT";
        }
        if (action.contains("RDV")) {
            return "RENDEZVOUS";
        }
        if (action.contains("USAGER")) {
            return "USAGER";
        }
        return "AUTRE";
    }

    private UUID extractEntityId(Object[] args, Object result) {
        if (result != null) {
            try {
                var idMethod = result.getClass().getMethod("id");
                Object id = idMethod.invoke(result);
                if (id instanceof UUID uuid) {
                    return uuid;
                }
            } catch (ReflectiveOperationException ignored) {
                // record DTO ou autre forme
            }
            try {
                var getId = result.getClass().getMethod("getId");
                Object id = getId.invoke(result);
                if (id instanceof UUID uuid) {
                    return uuid;
                }
            } catch (ReflectiveOperationException ignored) {
                // pas d'identifiant sur le résultat
            }
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof UUID uuid) {
                    return uuid;
                }
            }
        }
        return null;
    }

    private String safeSerialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{\"serializationError\":\"" + ex.getMessage() + "\"}";
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private UUID resolveUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        try {
            var method = principal.getClass().getMethod("getUserId");
            Object userId = method.invoke(principal);
            if (userId instanceof UUID uuid) {
                return uuid;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader("User-Agent");
    }

    private record AuditMetadata(String action, String entiteType) {
    }
}
