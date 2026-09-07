package com.consulos.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
public class PaymentConfig {

    @Bean
    @ConditionalOnProperty(prefix = "consulos.payment", name = "provider", havingValue = "log", matchIfMissing = true)
    PaymentGateway loggingPaymentGateway(PaymentProperties properties) {
        return new LoggingPaymentGateway(properties.getSimulateUrl());
    }

    @Bean
    @ConditionalOnProperty(prefix = "consulos.payment", name = "provider", havingValue = "stripe")
    PaymentGateway stripePaymentGateway(PaymentProperties properties) {
        return new StripePaymentGateway(properties);
    }

    @Bean
    WebSecurityCustomizer paymentWebhookSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/payments/webhook/**");
    }
}
