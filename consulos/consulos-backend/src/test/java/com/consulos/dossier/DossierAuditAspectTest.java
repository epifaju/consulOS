package com.consulos.dossier;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierAuditAspectTest {

    @Mock
    private JoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;
    @Mock
    private Auditable auditable;

    private final DossierAuditAspect aspect = new DossierAuditAspect();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void logAudit_withMdc_shouldNotThrow() throws Exception {
        Method method = DossierAuditAspect.class.getDeclaredMethod("logAudit", JoinPoint.class, Auditable.class);
        UUID argId = UUID.randomUUID();

        MDC.put("userId", UUID.randomUUID().toString());
        MDC.put("organisationId", UUID.randomUUID().toString());
        MDC.put("requestId", UUID.randomUUID().toString());

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(auditable.action()).thenReturn("DOSSIER_TRANSITION");
        when(joinPoint.getArgs()).thenReturn(new Object[]{argId, "note"});

        assertDoesNotThrow(() -> aspect.logAudit(joinPoint, auditable));
    }
}
