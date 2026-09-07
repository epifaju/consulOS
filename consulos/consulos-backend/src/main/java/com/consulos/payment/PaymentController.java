package com.consulos.payment;

import com.consulos.common.ApiResponse;
import com.consulos.payment.dto.PaymentCheckoutRequestDTO;
import com.consulos.payment.dto.PaymentSessionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.payment", name = "enabled", havingValue = "true")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentProperties properties;
    private final PaymentGateway paymentGateway;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('CAISSIER','SUPERVISEUR','ADMIN','AGENT')")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of(
                "enabled", properties.isEnabled(),
                "provider", paymentGateway.providerName()
        ));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('CAISSIER','ADMIN')")
    public ApiResponse<PaymentSessionDTO> checkout(@Valid @RequestBody PaymentCheckoutRequestDTO dto) {
        UUID organisationId = PaymentSecurityUtils.getOrganisationId();
        return ApiResponse.ok(paymentService.createCheckout(dto.encaissementId(), organisationId));
    }

    @GetMapping("/sessions/{providerSessionId}")
    @PreAuthorize("hasAnyRole('CAISSIER','ADMIN','SUPERVISEUR')")
    public ApiResponse<PaymentSessionDTO> getSession(@PathVariable String providerSessionId) {
        return ApiResponse.ok(paymentService.getSession(providerSessionId));
    }

    /** Confirmation simulateur (provider=log) — protégé JWT staff. */
    @PostMapping("/sessions/{providerSessionId}/complete")
    @PreAuthorize("hasAnyRole('CAISSIER','ADMIN')")
    public ApiResponse<PaymentSessionDTO> complete(@PathVariable String providerSessionId) {
        return ApiResponse.ok(paymentService.completeSession(providerSessionId));
    }
}
