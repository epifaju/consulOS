package com.consulos.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SmsProperties.class)
@ConditionalOnProperty(prefix = "consulos.sms", name = "enabled", havingValue = "true")
public class SmsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "consulos.sms", name = "provider", havingValue = "log", matchIfMissing = true)
    SmsGateway loggingSmsGateway() {
        return new LoggingSmsGateway();
    }

    @Bean
    @ConditionalOnProperty(prefix = "consulos.sms", name = "provider", havingValue = "twilio")
    SmsGateway twilioSmsGateway(SmsProperties properties, RestClient.Builder restClientBuilder) {
        return new TwilioSmsGateway(properties, restClientBuilder);
    }
}
