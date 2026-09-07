package com.consulos.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    String providerName();

    PaymentCheckoutResult createCheckout(
            String providerSessionId,
            BigDecimal amount,
            String currency,
            String description
    );
}
