package com.consulos.sms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TwilioSmsGatewayTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Test
    void send_whenIncompleteConfig_shouldFailWithConfigurationMessage() {
        SmsProperties properties = new SmsProperties();
        properties.getTwilio().setAccountSid("");
        properties.getTwilio().setAuthToken("");
        properties.setFromNumber("");

        TwilioSmsGateway gateway = new TwilioSmsGateway(properties, restClientBuilder);

        SmsSendResult result = gateway.send("+221770000000", "Test");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Configuration");
        assertThat(gateway.providerName()).isEqualTo("twilio");
    }
}
