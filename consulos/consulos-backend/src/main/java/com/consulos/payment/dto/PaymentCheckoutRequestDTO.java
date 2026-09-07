package com.consulos.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentCheckoutRequestDTO(@NotNull UUID encaissementId) {
}
