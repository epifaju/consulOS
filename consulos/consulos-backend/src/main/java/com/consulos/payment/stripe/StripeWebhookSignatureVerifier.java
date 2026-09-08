package com.consulos.payment.stripe;

import com.consulos.common.BusinessException;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Vérification signature Stripe webhook (v1) sans SDK.
 */
public final class StripeWebhookSignatureVerifier {

    private StripeWebhookSignatureVerifier() {
    }

    public static void verify(String payload, String stripeSignatureHeader, String webhookSecret, long toleranceSeconds) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BusinessException(
                    "STRIPE_WEBHOOK_NOT_CONFIGURED",
                    "Webhook secret Stripe non configuré",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (stripeSignatureHeader == null || stripeSignatureHeader.isBlank()) {
            throw new BusinessException(
                    "STRIPE_SIGNATURE_MISSING",
                    "En-tête Stripe-Signature manquant",
                    HttpStatus.BAD_REQUEST);
        }

        String timestamp = null;
        String signature = null;
        for (String part : stripeSignatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0])) {
                timestamp = kv[1];
            } else if ("v1".equals(kv[0])) {
                signature = kv[1];
            }
        }
        if (timestamp == null || signature == null) {
            throw new BusinessException(
                    "STRIPE_SIGNATURE_INVALID",
                    "Signature Stripe invalide",
                    HttpStatus.BAD_REQUEST);
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new BusinessException(
                    "STRIPE_SIGNATURE_INVALID",
                    "Timestamp signature invalide",
                    HttpStatus.BAD_REQUEST);
        }

        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - ts) > toleranceSeconds) {
            throw new BusinessException(
                    "STRIPE_SIGNATURE_EXPIRED",
                    "Signature Stripe expirée",
                    HttpStatus.BAD_REQUEST);
        }

        String signedPayload = timestamp + "." + payload;
        String expected = hmacSha256Hex(webhookSecret, signedPayload);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(
                    "STRIPE_SIGNATURE_MISMATCH",
                    "Signature Stripe non concordante",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new BusinessException(
                    "STRIPE_SIGNATURE_ERROR",
                    "Impossible de vérifier la signature Stripe",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
