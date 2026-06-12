package com.consulos.encaissement;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class EncaissementAuditAspect {

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint joinPoint, Auditable auditable) {
        log.info(
                "AUDIT encaissement action={} method={} userId={} organisationId={} requestId={}",
                auditable.action(),
                joinPoint.getSignature().getName(),
                MDC.get("userId"),
                MDC.get("organisationId"),
                MDC.get("requestId")
        );
    }
}
