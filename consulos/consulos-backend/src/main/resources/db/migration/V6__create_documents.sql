CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    dossier_id      UUID REFERENCES dossiers(id),
    usager_id       UUID REFERENCES usagers(id),
    nom_fichier     VARCHAR(255) NOT NULL,
    nom_original    VARCHAR(255) NOT NULL,
    type_document   VARCHAR(100) NOT NULL,
    mime_type       VARCHAR(100),
    taille_octets   BIGINT,
    chemin_stockage VARCHAR(500) NOT NULL,
    statut_controle VARCHAR(30) DEFAULT 'RECU',
    version         INTEGER DEFAULT 1,
    notes           TEXT,
    actif           BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id)
);

CREATE INDEX idx_documents_dossier ON documents(dossier_id) WHERE actif = true;
CREATE INDEX idx_documents_usager  ON documents(usager_id) WHERE actif = true;
