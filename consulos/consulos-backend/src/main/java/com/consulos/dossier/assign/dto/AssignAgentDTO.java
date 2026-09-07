package com.consulos.dossier.assign.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignAgentDTO(@NotNull UUID agentId) {
}
