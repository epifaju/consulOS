package com.consulos.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TwilioSmsGateway implements SmsGateway {

    private final SmsProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String providerName() {
        return "twilio";
    }

    @Override
    public SmsSendResult send(String toE164, String body) {
        String sid = properties.getTwilio().getAccountSid();
        String token = properties.getTwilio().getAuthToken();
        String from = properties.getFromNumber();
        if (!StringUtils.hasText(sid) || !StringUtils.hasText(token) || !StringUtils.hasText(from)) {
            return SmsSendResult.failed("Configuration Twilio incomplète (sid/token/from)");
        }

        try {
            String url = properties.getTwilio().getApiBase()
                    + "/2010-04-01/Accounts/" + sid + "/Messages.json";
            String basic = Base64.getEncoder()
                    .encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", toE164);
            form.add("From", from);
            form.add("Body", body);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("Authorization", "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            String providerRef = response != null && response.get("sid") != null
                    ? String.valueOf(response.get("sid"))
                    : null;
            return SmsSendResult.ok(providerRef);
        } catch (Exception ex) {
            log.warn("Échec envoi Twilio vers {}: {}", toE164, ex.getMessage());
            return SmsSendResult.failed(ex.getMessage());
        }
    }
}
