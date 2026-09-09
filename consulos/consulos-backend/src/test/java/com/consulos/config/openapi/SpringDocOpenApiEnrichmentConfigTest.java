package com.consulos.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringDocOpenApiEnrichmentConfigTest {

    private final SpringDocOpenApiEnrichmentConfig config = new SpringDocOpenApiEnrichmentConfig();

    @Test
    void operationCustomizer_shouldEnrichPublicLoginFromCatalog() throws Exception {
        OperationCustomizer customizer = config.consulosOperationCustomizer();
        Operation operation = new Operation();
        HandlerMethod handler = handlerMethod(AuthController.class, "login");

        Operation result = customizer.customize(operation, handler);

        assertThat(result.getTags()).containsExactly("01 — Authentification");
        assertThat(result.getSummary()).isEqualTo("Connexion utilisateur");
        assertThat(result.getDescription()).contains("access token JWT");
        assertThat(result.getSecurity()).isEmpty();
    }

    @Test
    void operationCustomizer_shouldNotClearSecurityForProtectedReporting() throws Exception {
        OperationCustomizer customizer = config.consulosOperationCustomizer();
        Operation operation = new Operation();
        HandlerMethod handler = handlerMethod(ReportingController.class, "mensuel");

        Operation result = customizer.customize(operation, handler);

        assertThat(result.getTags()).containsExactly("11 — Reporting");
        assertThat(result.getSummary()).isEqualTo("Reporting mensuel JSON");
        assertThat(result.getSecurity()).isNull();
    }

    @Test
    void operationCustomizer_shouldLeaveUnknownHandlerUntouched() throws Exception {
        OperationCustomizer customizer = config.consulosOperationCustomizer();
        Operation operation = new Operation().summary("existing");
        HandlerMethod handler = handlerMethod(UnknownController.class, "ghost");

        Operation result = customizer.customize(operation, handler);

        assertThat(result.getSummary()).isEqualTo("existing");
        assertThat(result.getTags()).isNull();
    }

    @Test
    void tagsCustomizer_shouldMergeCatalogDescriptions() {
        OpenApiCustomizer customizer = config.consulosTagsCustomizer();
        OpenAPI openApi = new OpenAPI();
        openApi.setTags(List.of(new Tag().name("pre-existing").description("keep me")));

        customizer.customise(openApi);

        assertThat(openApi.getTags())
                .extracting(Tag::getName)
                .contains("pre-existing", "01 — Authentification", "11 — Reporting");
        assertThat(openApi.getTags())
                .filteredOn(t -> "01 — Authentification".equals(t.getName()))
                .singleElement()
                .extracting(Tag::getDescription)
                .asString()
                .contains("JWT");
    }

    private static HandlerMethod handlerMethod(Class<?> beanType, String methodName) throws Exception {
        Method method = beanType.getDeclaredMethod(methodName);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);
        when(handlerMethod.getBeanType()).thenAnswer(invocation -> beanType);
        when(handlerMethod.getMethod()).thenReturn(method);
        return handlerMethod;
    }

    /** Nested stubs so {@code getSimpleName()} matches catalog keys. */
    static class AuthController {
        public void login() {
        }
    }

    static class ReportingController {
        public void mensuel() {
        }
    }

    static class UnknownController {
        public void ghost() {
        }
    }
}
