package com.consulos.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSessionDTO(
        UUID id,
        UUID encaissementId,
        String provider,
        String providerSessionId,
        String status,
        BigDecimal amount,
        String currency,
        String checkoutUrl
) {
}
