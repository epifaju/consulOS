package com.consulos.dossier;

import com.consulos.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WorkflowEngine workflowEngine;
    private JsonNode workflowConfig;

    @BeforeEach
    void setUp() throws Exception {
        workflowEngine = new WorkflowEngine();
        workflowConfig = objectMapper.readTree("""
                {
                  "transitions": {
                    "BROUILLON": ["SOUMIS"],
                    "SOUMIS": ["INCOMPLET", "EN_REVUE"],
                    "INCOMPLET": ["SOUMIS"],
                    "EN_REVUE": ["VALIDE", "REJETE", "INCOMPLET"],
                    "VALIDE": ["CLOTURE"],
                    "REJETE": ["CLOTURE", "SOUMIS"],
                    "CLOTURE": []
                  },
                  "superviseurRequired": ["VALIDE", "REJETE"]
                }
                """);
    }

    @Test
    void brouillonToSoumisByAgent_shouldSucceed() {
        assertThatCode(() -> workflowEngine.validateTransition(
                DossierStatut.BROUILLON,
                DossierStatut.SOUMIS,
                workflowConfig,
                "ROLE_AGENT",
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void enRevueToValideByAgent_shouldThrowForbidden() {
        assertThatThrownBy(() -> workflowEngine.validateTransition(
                DossierStatut.EN_REVUE,
                DossierStatut.VALIDE,
                workflowConfig,
                "ROLE_AGENT",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ROLE_INSUFFISANT")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void enRevueToValideBySuperviseur_shouldSucceed() {
        assertThatCode(() -> workflowEngine.validateTransition(
                DossierStatut.EN_REVUE,
                DossierStatut.VALIDE,
                workflowConfig,
                "ROLE_SUPERVISEUR",
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void enRevueToRejeteWithoutMotif_shouldThrowBadRequest() {
        assertThatThrownBy(() -> workflowEngine.validateTransition(
                DossierStatut.EN_REVUE,
                DossierStatut.REJETE,
                workflowConfig,
                "ROLE_SUPERVISEUR",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "MOTIF_REQUIS")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void clotureToAnyStatus_shouldThrowInvalidTransition() {
        assertThatThrownBy(() -> workflowEngine.validateTransition(
                DossierStatut.CLOTURE,
                DossierStatut.SOUMIS,
                workflowConfig,
                "ROLE_ADMIN",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "TRANSITION_INVALIDE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
}
