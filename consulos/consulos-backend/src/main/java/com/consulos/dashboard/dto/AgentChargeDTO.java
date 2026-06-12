package com.consulos.dashboard.dto;

import java.util.UUID;

public record AgentChargeDTO(
        UUID agentId,
        String agentNom,
        long nbDossiersEnCours,
        long nbRdvAujourdhui
) {
}
