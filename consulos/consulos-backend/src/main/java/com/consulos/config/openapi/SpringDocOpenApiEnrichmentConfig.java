package com.consulos.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SpringDocOpenApiEnrichmentConfig {

    @Bean
    public OperationCustomizer consulosOperationCustomizer() {
        return (operation, handlerMethod) -> {
            String key = handlerKey(handlerMethod);
            OpenApiOperationCatalog.resolve(key).ifPresent(meta -> {
                if (operation.getTags() == null || operation.getTags().isEmpty()) {
                    operation.setTags(List.of(meta.tag()));
                }
                if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                    operation.setSummary(meta.summary());
                }
                if (operation.getDescription() == null || operation.getDescription().isBlank()) {
                    operation.setDescription(meta.description());
                }
                if (meta.publicEndpoint()) {
                    operation.setSecurity(List.of());
                }
            });
            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer consulosTagsCustomizer() {
        return openApi -> mergeTags(openApi);
    }

    private static void mergeTags(OpenAPI openApi) {
        Set<String> existing = new LinkedHashSet<>();
        if (openApi.getTags() != null) {
            openApi.getTags().forEach(tag -> existing.add(tag.getName()));
        }
        List<Tag> merged = openApi.getTags() != null ? new ArrayList<>(openApi.getTags()) : new ArrayList<>();
        OpenApiOperationCatalog.TAG_DESCRIPTIONS.forEach((name, description) -> {
            if (!existing.contains(name)) {
                merged.add(new Tag().name(name).description(description));
            }
        });
        openApi.setTags(merged);
    }

    private static String handlerKey(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    }
}
