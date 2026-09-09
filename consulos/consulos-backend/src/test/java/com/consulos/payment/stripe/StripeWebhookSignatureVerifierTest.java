package com.consulos.payment.stripe;

import com.consulos.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeWebhookSignatureVerifierTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}";

    @Test
    void verify_whenValidSignature_shouldPass() {
        long ts = System.currentTimeMillis() / 1000L;
        String header = "t=" + ts + ",v1=" + hmac(SECRET, ts + "." + PAYLOAD);

        assertThatCode(() -> StripeWebhookSignatureVerifier.verify(PAYLOAD, header, SECRET, 300))
                .doesNotThrowAnyException();
    }

    @Test
    void verify_whenSecretBlank_shouldThrowNotConfigured() {
        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify(PAYLOAD, "t=1,v1=abc", "  ", 300))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("STRIPE_WEBHOOK_NOT_CONFIGURED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void verify_whenHeaderMissing_shouldThrow() {
        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify(PAYLOAD, null, SECRET, 300))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("STRIPE_SIGNATURE_MISSING"));
    }

    @Test
    void verify_whenHeaderMalformed_shouldThrowInvalid() {
        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify(PAYLOAD, "foo=bar", SECRET, 300))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("STRIPE_SIGNATURE_INVALID"));
    }

    @Test
    void verify_whenTimestampExpired_shouldThrow() {
        long ts = (System.currentTimeMillis() / 1000L) - 10_000;
        String header = "t=" + ts + ",v1=" + hmac(SECRET, ts + "." + PAYLOAD);

        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify(PAYLOAD, header, SECRET, 300))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("STRIPE_SIGNATURE_EXPIRED"));
    }

    @Test
    void verify_whenSignatureMismatch_shouldThrow() {
        long ts = System.currentTimeMillis() / 1000L;
        String header = "t=" + ts + ",v1=deadbeef";

        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify(PAYLOAD, header, SECRET, 300))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("STRIPE_SIGNATURE_MISMATCH"));
    }

    private static String hmac(String secret, String signedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
