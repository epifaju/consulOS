package com.consulos.dossier;

import com.consulos.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class WorkflowEngine {

    public void validateTransition(
            DossierStatut statutActuel,
            DossierStatut nouveauStatut,
            JsonNode workflowConfig,
            String userRole,
            String motif
    ) {
        if (workflowConfig == null || !workflowConfig.has("transitions")) {
            throw new BusinessException(
                    "TRANSITION_INVALIDE",
                    "Configuration workflow invalide",
                    HttpStatus.BAD_REQUEST
            );
        }

        JsonNode transitions = workflowConfig.get("transitions");
        JsonNode allowed = transitions.get(statutActuel.name());
        if (allowed == null || !allowed.isArray()) {
            throw new BusinessException(
                    "TRANSITION_INVALIDE",
                    "Aucune transition possible depuis le statut " + statutActuel,
                    HttpStatus.BAD_REQUEST
            );
        }

        boolean transitionAutorisee = false;
        for (JsonNode node : allowed) {
            if (nouveauStatut.name().equals(node.asText())) {
                transitionAutorisee = true;
                break;
            }
        }
        if (!transitionAutorisee) {
            throw new BusinessException(
                    "TRANSITION_INVALIDE",
                    "Transition " + statutActuel + " → " + nouveauStatut + " non autorisée",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (workflowConfig.has("superviseurRequired") && workflowConfig.get("superviseurRequired").isArray()) {
            for (JsonNode required : workflowConfig.get("superviseurRequired")) {
                if (nouveauStatut.name().equals(required.asText())) {
                    if (!hasSuperviseurRole(userRole)) {
                        throw new BusinessException(
                                "ROLE_INSUFFISANT",
                                "Rôle superviseur requis pour cette transition",
                                HttpStatus.FORBIDDEN
                        );
                    }
                    break;
                }
            }
        }

        if ((nouveauStatut == DossierStatut.REJETE || nouveauStatut == DossierStatut.INCOMPLET)
                && !StringUtils.hasText(motif)) {
            throw new BusinessException(
                    "MOTIF_REQUIS",
                    "Un motif est obligatoire pour cette transition",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private boolean hasSuperviseurRole(String userRole) {
        if (!StringUtils.hasText(userRole)) {
            return false;
        }
        return userRole.contains("SUPERVISEUR") || userRole.contains("ADMIN");
    }
}
