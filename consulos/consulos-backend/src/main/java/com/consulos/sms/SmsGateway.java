package com.consulos.sms;

public interface SmsGateway {

    String providerName();

    SmsSendResult send(String toE164, String body);
}
