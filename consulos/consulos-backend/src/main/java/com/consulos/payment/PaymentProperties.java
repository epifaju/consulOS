package com.consulos.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consulos.payment")
public class PaymentProperties {

    private boolean enabled = false;

    /** log (simulateur) | stripe | paydunya */
    private String provider = "log";

    private String successUrl = "http://localhost:3000/caisse?payment=success";

    private String cancelUrl = "http://localhost:3000/caisse?payment=cancel";

    private String simulateUrl = "http://localhost:3000/caisse/paiement-en-ligne";

    private final Stripe stripe = new Stripe();

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

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getSimulateUrl() {
        return simulateUrl;
    }

    public void setSimulateUrl(String simulateUrl) {
        this.simulateUrl = simulateUrl;
    }

    public Stripe getStripe() {
        return stripe;
    }

    public static class Stripe {
        private String secretKey = "";
        private String webhookSecret = "";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }
}
