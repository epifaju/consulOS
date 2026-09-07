package com.consulos.sms;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Provider par défaut : journalise le SMS (dev / démo sans compte Twilio).
 */
@Slf4j
public class LoggingSmsGateway implements SmsGateway {

    @Override
    public String providerName() {
        return "log";
    }

    @Override
    public SmsSendResult send(String toE164, String body) {
        String ref = "log-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SMS-LOG] to={} ref={} body={}", toE164, ref, body);
        return SmsSendResult.ok(ref);
    }
}
