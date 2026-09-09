package com.consulos.audit;

import com.consulos.auth.ConsulosUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectUnitTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AuditAspect auditAspect;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditLogRepository, objectMapper);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aroundAuditable_success_shouldPersistWithInferredEntiteTypeAndForwardedIp() throws Throwable {
        stubAnnotatedMethod("createDossier");
        UUID resultId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MDC.put("organisationId", orgId.toString());
        MDC.put("userId", userId.toString());

        when(joinPoint.getArgs()).thenReturn(new Object[]{"payload"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"payload"});
        when(joinPoint.proceed()).thenReturn(new ResultWithId(resultId));

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");

        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Object result = auditAspect.aroundAuditable(joinPoint);

        assertThat(result).isInstanceOf(ResultWithId.class);
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getOrganisationId()).isEqualTo(orgId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAction()).isEqualTo("DOSSIER_CREATE");
        assertThat(saved.getEntiteType()).isEqualTo("DOSSIER");
        assertThat(saved.getEntiteId()).isEqualTo(resultId);
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(saved.getUserAgent()).isEqualTo("JUnit");
        assertThat(saved.getNouvelEtat()).contains(resultId.toString());
    }

    @Test
    void aroundAuditable_whenProceedFails_shouldPersistFailurePayloadAndRethrow() throws Throwable {
        stubAnnotatedMethod("payEncaissement");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> auditAspect.aroundAuditable(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntiteType()).isEqualTo("ENCAISSEMENT");
        assertThat(captor.getValue().getNouvelEtat()).contains("\"success\":false");
        assertThat(captor.getValue().getNouvelEtat()).contains("IllegalStateException");
    }

    @Test
    void aroundAuditable_blankAction_shouldNotPersist() throws Throwable {
        stubAnnotatedMethod("blankAction");
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(auditAspect.aroundAuditable(joinPoint)).isEqualTo("ok");
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void aroundAuditable_shouldInferDocumentRdvUsagerAndAutre() throws Throwable {
        assertEntiteType("uploadDocument", "DOCUMENT");
        assertEntiteType("confirmRdv", "RENDEZVOUS");
        assertEntiteType("createUsager", "USAGER");
        assertEntiteType("miscAction", "AUTRE");
        assertEntiteType("paiementStripe", "ENCAISSEMENT");
    }

    @Test
    void aroundAuditable_explicitEntiteType_shouldWin() throws Throwable {
        stubAnnotatedMethod("explicitType");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.proceed()).thenReturn(null);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntiteType()).isEqualTo("CUSTOM");
    }

    @Test
    void aroundAuditable_shouldExtractIdFromGetIdAndUuidArg() throws Throwable {
        stubAnnotatedMethod("miscAction");
        UUID argId = UUID.randomUUID();
        UUID beanId = UUID.randomUUID();
        when(joinPoint.getArgs()).thenReturn(new Object[]{argId});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.proceed()).thenReturn(new BeanWithGetId(beanId));
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntiteId()).isEqualTo(beanId);
    }

    @Test
    void aroundAuditable_whenResultHasNoId_shouldUseUuidArg() throws Throwable {
        stubAnnotatedMethod("miscAction");
        UUID argId = UUID.randomUUID();
        when(joinPoint.getArgs()).thenReturn(new Object[]{argId, "note"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "note"});
        when(joinPoint.proceed()).thenReturn("plain");
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntiteId()).isEqualTo(argId);
    }

    @Test
    void aroundAuditable_shouldRedactPasswordAndMultipartArgs() throws Throwable {
        stubAnnotatedMethod("miscAction");
        when(joinPoint.getArgs()).thenReturn(new Object[]{
                new PasswordPayload("secret"),
                new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes())
        });
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"pwd", "file"});
        when(joinPoint.proceed()).thenReturn(null);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAncienEtat()).contains("[REDACTED]");
        assertThat(captor.getValue().getAncienEtat()).doesNotContain("secret");
    }

    @Test
    void aroundAuditable_invalidMdcUuid_shouldFallbackToSecurityContextUserId() throws Throwable {
        stubAnnotatedMethod("miscAction");
        UUID securityUserId = UUID.randomUUID();
        MDC.put("organisationId", "not-a-uuid");
        MDC.put("userId", " ");
        ConsulosUserDetails details = new ConsulosUserDetails(
                securityUserId, "u@c.local", "n/a", UUID.randomUUID(), "ROLE_ADMIN", true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );

        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.proceed()).thenReturn(null);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganisationId()).isNull();
        assertThat(captor.getValue().getUserId()).isEqualTo(securityUserId);
    }

    @Test
    void aroundAuditable_whenSaveFails_shouldSwallowAndReturnResult() throws Throwable {
        stubAnnotatedMethod("miscAction");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenThrow(new RuntimeException("db down"));

        assertThat(auditAspect.aroundAuditable(joinPoint)).isEqualTo("ok");
    }

    @Test
    void aroundAuditable_whenSerializeFails_shouldStoreSerializationError() throws Throwable {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json boom"));
        auditAspect = new AuditAspect(auditLogRepository, failingMapper);

        stubAnnotatedMethod("miscAction");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"x"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"x"});
        when(joinPoint.proceed()).thenReturn("y");
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAncienEtat()).contains("serializationError");
        assertThat(captor.getValue().getNouvelEtat()).contains("serializationError");
    }

    private void assertEntiteType(String methodName, String expectedType) throws Throwable {
        clearInvocations(auditLogRepository, joinPoint, methodSignature);
        stubAnnotatedMethod(methodName);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.proceed()).thenReturn(null);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        auditAspect.aroundAuditable(joinPoint);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntiteType()).isEqualTo(expectedType);
    }

    private void stubAnnotatedMethod(String methodName) throws NoSuchMethodException {
        Method method = AuditableFixtures.class.getDeclaredMethod(methodName);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    static final class AuditableFixtures {
        @Auditable(action = "DOSSIER_CREATE")
        void createDossier() {
        }

        @Auditable(action = "ENCAISSEMENT_PAY")
        void payEncaissement() {
        }

        @Auditable(action = "DOCUMENT_UPLOAD")
        void uploadDocument() {
        }

        @Auditable(action = "RDV_CONFIRM")
        void confirmRdv() {
        }

        @Auditable(action = "USAGER_CREATE")
        void createUsager() {
        }

        @Auditable(action = "MISC_OP")
        void miscAction() {
        }

        @Auditable(action = "PAIEMENT_STRIPE")
        void paiementStripe() {
        }

        @Auditable(action = " ")
        void blankAction() {
        }

        @Auditable(action = "X", entiteType = "CUSTOM")
        void explicitType() {
        }
    }

    record ResultWithId(UUID id) {
    }

    static final class BeanWithGetId {
        private final UUID id;

        BeanWithGetId(UUID id) {
            this.id = id;
        }

        public UUID getId() {
            return id;
        }
    }

    static final class PasswordPayload {
        private final String value;

        PasswordPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
