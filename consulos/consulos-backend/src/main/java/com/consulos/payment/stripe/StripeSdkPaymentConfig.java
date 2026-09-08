package com.consulos.payment.stripe;

import com.consulos.payment.PaymentGateway;
import com.consulos.payment.PaymentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
public class StripeSdkPaymentConfig {

    @Bean
    @ConditionalOnProperty(prefix = "consulos.payment", name = "provider", havingValue = "stripe-sdk")
    PaymentGateway stripeSdkHttpPaymentGateway(
            PaymentProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        return new StripeSdkHttpPaymentGateway(properties, restClientBuilder);
    }
}
