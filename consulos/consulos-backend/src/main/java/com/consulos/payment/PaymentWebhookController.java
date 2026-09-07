package com.consulos.payment;

import com.consulos.common.ApiResponse;
import com.consulos.payment.dto.PaymentSessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook public (Security ignorée). Provider=log : body { "sessionId": "pay_..." }.
 */
@Slf4j
@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/log")
    public ApiResponse<PaymentSessionDTO> logWebhook(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = body.get("providerSessionId");
        }
        log.info("Webhook paiement log reçu session={}", sessionId);
        return ApiResponse.ok(paymentService.completeSession(sessionId));
    }
}
