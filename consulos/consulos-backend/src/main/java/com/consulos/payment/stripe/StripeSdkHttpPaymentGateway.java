package com.consulos.payment.stripe;

import com.consulos.common.BusinessException;
import com.consulos.payment.PaymentCheckoutResult;
import com.consulos.payment.PaymentGateway;
import com.consulos.payment.PaymentProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Checkout Sessions Stripe via API HTTP (sans dépendance SDK).
 * Activer avec {@code consulos.payment.provider=stripe-sdk} + clé secrète.
 */
public class StripeSdkHttpPaymentGateway implements PaymentGateway {

    private final PaymentProperties properties;
    private final RestClient restClient;

    public StripeSdkHttpPaymentGateway(PaymentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl("https://api.stripe.com").build();
    }

    @Override
    public String providerName() {
        return "stripe-sdk";
    }

    @Override
    public PaymentCheckoutResult createCheckout(
            String providerSessionId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        String secret = properties.getStripe().getSecretKey();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(
                    "STRIPE_NOT_CONFIGURED",
                    "STRIPE_SECRET_KEY / consulos.payment.stripe.secret-key requis pour stripe-sdk",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String cur = currency != null ? currency.toLowerCase() : "eur";
        long amountCents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("success_url", appendSession(properties.getSuccessUrl(), providerSessionId));
        form.add("cancel_url", appendSession(properties.getCancelUrl(), providerSessionId));
        form.add("client_reference_id", providerSessionId);
        form.add("metadata[consulos_session_id]", providerSessionId);
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", cur);
        form.add("line_items[0][price_data][unit_amount]", String.valueOf(amountCents));
        form.add("line_items[0][price_data][product_data][name]",
                description != null && !description.isBlank() ? description : "ConsulOS paiement");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1/checkout/sessions")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(h -> h.setBasicAuth(secret, ""))
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("url") == null) {
                throw new BusinessException(
                        "STRIPE_CHECKOUT_FAILED",
                        "Réponse Stripe invalide",
                        HttpStatus.BAD_GATEWAY);
            }

            String checkoutUrl = String.valueOf(response.get("url"));
            return new PaymentCheckoutResult(providerSessionId, checkoutUrl, amount, cur.toUpperCase());
        } catch (RestClientResponseException ex) {
            throw new BusinessException(
                    "STRIPE_CHECKOUT_FAILED",
                    "Stripe Checkout échoué: " + ex.getResponseBodyAsString(),
                    HttpStatus.BAD_GATEWAY);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    "STRIPE_CHECKOUT_FAILED",
                    "Stripe Checkout échoué: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private static String appendSession(String baseUrl, String sessionId) {
        String base = baseUrl != null && !baseUrl.isBlank()
                ? baseUrl
                : "http://localhost:3010/caisse/paiement-en-ligne";
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + "session=" + sessionId;
    }
}
