package com.consulos.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consulos.sms")
public class SmsProperties {

    /**
     * Désactivé par défaut (feature optionnelle Phase 2).
     */
    private boolean enabled = false;

    /** log (dev / MailHog-like) ou twilio */
    private String provider = "log";

    private String fromNumber = "";

    private final Twilio twilio = new Twilio();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public Twilio getTwilio() {
        return twilio;
    }

    public static class Twilio {
        private String accountSid = "";
        private String authToken = "";
        private String apiBase = "https://api.twilio.com";

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getApiBase() {
            return apiBase;
        }

        public void setApiBase(String apiBase) {
            this.apiBase = apiBase;
        }
    }
}
