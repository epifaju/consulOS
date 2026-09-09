package com.consulos.sms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingSmsGatewayTest {

    private final LoggingSmsGateway gateway = new LoggingSmsGateway();

    @Test
    void providerName_shouldBeLog() {
        assertThat(gateway.providerName()).isEqualTo("log");
    }

    @Test
    void send_shouldReturnSuccessWithLogRef() {
        SmsSendResult result = gateway.send("+221770000000", "Bonjour");

        assertThat(result.success()).isTrue();
        assertThat(result.providerRef()).startsWith("log-");
        assertThat(result.errorMessage()).isNull();
    }
}
