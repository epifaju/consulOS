package com.consulos.config.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiOperationCatalogTest {

    @Test
    void resolve_login_shouldBePublicWithAuthentificationTag() {
        var meta = OpenApiOperationCatalog.resolve("AuthController#login");

        assertThat(meta).isPresent();
        assertThat(meta.get().publicEndpoint()).isTrue();
        assertThat(meta.get().tag()).contains("Authentification");
        assertThat(meta.get().summary()).isEqualTo("Connexion utilisateur");
    }

    @Test
    void resolve_reportingMensuel_shouldNotBePublic() {
        var meta = OpenApiOperationCatalog.resolve("ReportingController#mensuel");

        assertThat(meta).isPresent();
        assertThat(meta.get().publicEndpoint()).isFalse();
        assertThat(meta.get().tag()).isEqualTo("11 — Reporting");
    }

    @Test
    void resolve_unknownKey_shouldBeEmpty() {
        assertThat(OpenApiOperationCatalog.resolve("UnknownController#ghost")).isEmpty();
    }

    @Test
    void tagDescriptions_shouldContainKnownAuthentificationTag() {
        assertThat(OpenApiOperationCatalog.TAG_DESCRIPTIONS).isNotEmpty();
        assertThat(OpenApiOperationCatalog.TAG_DESCRIPTIONS)
                .containsKey("01 — Authentification");
        assertThat(OpenApiOperationCatalog.TAG_DESCRIPTIONS.get("01 — Authentification"))
                .contains("JWT");
    }

    @Test
    void resolve_portalAndPaymentWebhookKeys_shouldBePresentWhenCatalogued() {
        var portal = OpenApiOperationCatalog.resolve("PortalController#requestAccess");
        assertThat(portal).isPresent();
        assertThat(portal.get().publicEndpoint()).isTrue();
        assertThat(portal.get().tag()).contains("Portail");

        var webhook = OpenApiOperationCatalog.resolve("PaymentWebhookController#logWebhook");
        assertThat(webhook).isPresent();
        assertThat(webhook.get().publicEndpoint()).isTrue();
        assertThat(webhook.get().tag()).contains("Paiements");
    }
}
