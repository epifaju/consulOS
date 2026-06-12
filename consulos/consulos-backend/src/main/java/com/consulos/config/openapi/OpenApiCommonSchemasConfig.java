package com.consulos.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiCommonSchemasConfig {

    @Bean
    public OpenApiCustomizer consulosCommonSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                return;
            }
            openApi.getComponents().addSchemas("ApiResponseEnvelope", apiResponseSchema());
            openApi.getComponents().addSchemas("PageResponseEnvelope", pageResponseSchema());
        };
    }

    private static Schema<?> apiResponseSchema() {
        return new ObjectSchema()
                .addProperty("success", new StringSchema().example("true").description("Indique si la requête a réussi"))
                .addProperty("data", new ObjectSchema().description("Charge utile métier"))
                .addProperty("message", new StringSchema().description("Message informatif optionnel"))
                .addProperty("errorCode", new StringSchema().description("Code erreur métier si success=false"));
    }

    private static Schema<?> pageResponseSchema() {
        return new ObjectSchema()
                .addProperty("content", new ObjectSchema().description("Éléments de la page courante"))
                .addProperty("page", new StringSchema().example("0"))
                .addProperty("size", new StringSchema().example("20"))
                .addProperty("totalElements", new StringSchema().example("142"))
                .addProperty("totalPages", new StringSchema().example("8"));
    }
}
