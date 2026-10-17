CREATE TABLE dossier_checklist_statuts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    dossier_id      UUID NOT NULL REFERENCES dossiers(id),
    code            VARCHAR(100) NOT NULL,
    valide          BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    UNIQUE (dossier_id, code)
);

CREATE INDEX idx_dossier_checklist_org_dossier
    ON dossier_checklist_statuts (organisation_id, dossier_id);
