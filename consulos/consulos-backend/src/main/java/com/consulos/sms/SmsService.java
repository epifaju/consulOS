package com.consulos.sms;

import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.sms", name = "enabled", havingValue = "true")
public class SmsService {

    private final SmsGateway smsGateway;
    private final SmsLogRepository smsLogRepository;
    private final UsagerRepository usagerRepository;
    private final SmsProperties properties;

    @Transactional
    public void sendRdvSms(
            UUID organisationId,
            UUID usagerId,
            UUID rdvId,
            String purpose,
            String body
    ) {
        UsagerEntity usager = usagerRepository.findById(usagerId).orElse(null);
        if (usager == null || !StringUtils.hasText(usager.getTelephone())) {
            log.info("SMS {} ignoré — pas de téléphone pour usager {}", purpose, usagerId);
            return;
        }
        String to = normalizePhone(usager.getTelephone());
        if (!StringUtils.hasText(to)) {
            log.info("SMS {} ignoré — téléphone invalide pour usager {}", purpose, usagerId);
            return;
        }

        SmsSendResult result = smsGateway.send(to, body);
        persistLog(organisationId, to, body, purpose, rdvId, result);
    }

    private void persistLog(
            UUID organisationId,
            String recipient,
            String body,
            String purpose,
            UUID rdvId,
            SmsSendResult result
    ) {
        SmsLogEntity entity = new SmsLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setOrganisationId(organisationId);
        entity.setRecipient(recipient);
        entity.setMessageBody(body);
        entity.setPurpose(purpose);
        entity.setProvider(smsGateway.providerName());
        entity.setStatus(result.success() ? "SENT" : "FAILED");
        entity.setProviderRef(result.providerRef());
        entity.setErrorMessage(result.errorMessage());
        entity.setRelatedRdvId(rdvId);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        smsLogRepository.save(entity);
    }

    static String normalizePhone(String raw) {
        String trimmed = raw.trim().replace(" ", "").replace("-", "");
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2);
        }
        if (trimmed.matches("^\\+[1-9]\\d{7,14}$") || trimmed.matches("^[1-9]\\d{7,14}$")) {
            return trimmed.startsWith("+") ? trimmed : "+" + trimmed;
        }
        return trimmed.startsWith("+") ? trimmed : null;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }
}
