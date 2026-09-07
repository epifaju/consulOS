-- Phase 2 M11 — portail usager (magic link + session)
CREATE TABLE portal_access_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    dossier_id      UUID NOT NULL REFERENCES dossiers(id),
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    token_hash      VARCHAR(64) NOT NULL UNIQUE,
    token_type      VARCHAR(20) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_portal_tokens_lookup
    ON portal_access_tokens (token_hash, token_type)
    WHERE consumed_at IS NULL;

CREATE INDEX idx_portal_tokens_dossier
    ON portal_access_tokens (dossier_id, created_at DESC);
