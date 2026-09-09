package com.consulos;

import com.consulos.document.DocumentAuditAspect;
import com.consulos.encaissement.EncaissementAuditAspect;
import com.consulos.rendezvous.RdvAuditAspect;
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
class DomainLoggingAuditAspectsTest {

    @Mock
    private JoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;
    @Mock
    private com.consulos.rendezvous.Auditable rdvAuditable;
    @Mock
    private com.consulos.encaissement.Auditable encAuditable;
    @Mock
    private com.consulos.document.Auditable docAuditable;

    private final RdvAuditAspect rdvAspect = new RdvAuditAspect();
    private final EncaissementAuditAspect encAspect = new EncaissementAuditAspect();
    private final DocumentAuditAspect docAspect = new DocumentAuditAspect();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void rdvAuditAspect_logAudit_shouldNotThrow() throws Exception {
        Method method = RdvAuditAspect.class.getDeclaredMethod(
                "logAudit", JoinPoint.class, com.consulos.rendezvous.Auditable.class);
        stubJoinPoint(method);
        when(rdvAuditable.action()).thenReturn("RDV_CREATE");

        assertDoesNotThrow(() -> rdvAspect.logAudit(joinPoint, rdvAuditable));
    }

    @Test
    void encaissementAuditAspect_logAudit_shouldNotThrow() throws Exception {
        Method method = EncaissementAuditAspect.class.getDeclaredMethod(
                "logAudit", JoinPoint.class, com.consulos.encaissement.Auditable.class);
        stubJoinPoint(method);
        when(encAuditable.action()).thenReturn("ENCAISSEMENT_CREATE");

        assertDoesNotThrow(() -> encAspect.logAudit(joinPoint, encAuditable));
    }

    @Test
    void documentAuditAspect_logAudit_shouldNotThrow() throws Exception {
        Method method = DocumentAuditAspect.class.getDeclaredMethod(
                "logAudit", JoinPoint.class, com.consulos.document.Auditable.class);
        stubJoinPoint(method);
        when(docAuditable.action()).thenReturn("DOCUMENT_UPLOAD");

        assertDoesNotThrow(() -> docAspect.logAudit(joinPoint, docAuditable));
    }

    private void stubJoinPoint(Method method) {
        MDC.put("userId", UUID.randomUUID().toString());
        MDC.put("organisationId", UUID.randomUUID().toString());
        MDC.put("requestId", UUID.randomUUID().toString());
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(method.getName());
    }
}
