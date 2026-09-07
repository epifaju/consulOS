package com.consulos.sms;

public record SmsSendResult(boolean success, String providerRef, String errorMessage) {

    public static SmsSendResult ok(String providerRef) {
        return new SmsSendResult(true, providerRef, null);
    }

    public static SmsSendResult failed(String errorMessage) {
        return new SmsSendResult(false, null, errorMessage);
    }
}
