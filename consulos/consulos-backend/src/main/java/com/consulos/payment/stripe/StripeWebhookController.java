package com.consulos.payment.stripe;

import com.consulos.common.ApiResponse;
import com.consulos.common.BusinessException;
import com.consulos.payment.PaymentProperties;
import com.consulos.payment.PaymentService;
import com.consulos.payment.dto.PaymentSessionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
@Tag(name = "13 — Paiements en ligne", description = "Webhook Stripe (Vague Stripe)")
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;

    @PostMapping("/stripe")
    @Operation(summary = "Webhook Stripe Checkout (signature v1)")
    public ApiResponse<PaymentSessionDTO> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature
    ) {
        StripeWebhookSignatureVerifier.verify(
                payload,
                stripeSignature,
                properties.getStripe().getWebhookSecret(),
                300
        );

        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText("");
            if (!"checkout.session.completed".equals(type)
                    && !"checkout.session.async_payment_succeeded".equals(type)) {
                log.info("Webhook Stripe ignoré type={}", type);
                return ApiResponse.ok(null, "ignored:" + type);
            }

            JsonNode obj = root.path("data").path("object");
            String consulosSessionId = obj.path("metadata").path("consulos_session_id").asText(null);
            if (consulosSessionId == null || consulosSessionId.isBlank()) {
                consulosSessionId = obj.path("client_reference_id").asText(null);
            }
            if (consulosSessionId == null || consulosSessionId.isBlank()) {
                throw new BusinessException(
                        "STRIPE_SESSION_UNKNOWN",
                        "Impossible de résoudre la session ConsulOS depuis le webhook",
                        HttpStatus.BAD_REQUEST);
            }

            log.info("Webhook Stripe checkout.session.completed session={}", consulosSessionId);
            return ApiResponse.ok(paymentService.completeSession(consulosSessionId));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    "STRIPE_WEBHOOK_PARSE_FAILED",
                    "Payload Stripe invalide",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
