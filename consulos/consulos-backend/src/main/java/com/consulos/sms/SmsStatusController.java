package com.consulos.sms;

import com.consulos.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.sms", name = "enabled", havingValue = "true")
public class SmsStatusController {

    private final SmsProperties properties;
    private final SmsGateway smsGateway;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISEUR')")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of(
                "enabled", properties.isEnabled(),
                "provider", smsGateway.providerName(),
                "fromNumberConfigured", properties.getFromNumber() != null && !properties.getFromNumber().isBlank()
        ));
    }
}
