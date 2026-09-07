package com.consulos.payment;

import java.math.BigDecimal;

public record PaymentCheckoutResult(
        String providerSessionId,
        String checkoutUrl,
        BigDecimal amount,
        String currency
) {
}
