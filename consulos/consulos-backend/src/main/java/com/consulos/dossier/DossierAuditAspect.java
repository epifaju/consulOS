package com.consulos.dossier;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class DossierAuditAspect {

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint joinPoint, Auditable auditable) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String userId = MDC.get("userId");
        String organisationId = MDC.get("organisationId");
        String requestId = MDC.get("requestId");

        log.info(
                "AUDIT dossier action={} method={} userId={} organisationId={} requestId={} args={}",
                auditable.action(),
                signature.getMethod().getName(),
                userId,
                organisationId,
                requestId,
                sanitizeArgs(joinPoint.getArgs())
        );
    }

    private String sanitizeArgs(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> arg instanceof UUID uuid ? uuid.toString() : String.valueOf(arg))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
