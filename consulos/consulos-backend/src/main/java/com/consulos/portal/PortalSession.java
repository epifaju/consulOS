package com.consulos.portal;

import java.util.UUID;

public record PortalSession(
        UUID organisationId,
        UUID dossierId,
        UUID usagerId,
        String dossierReference
) {
}
