CREATE SEQUENCE dossier_seq START 1;

CREATE TABLE types_service (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    code            VARCHAR(50) NOT NULL,
    libelle         VARCHAR(200) NOT NULL,
    delai_jours     INTEGER DEFAULT 30,
    tarif_defaut    DECIMAL(10,2),
    workflow_config JSONB DEFAULT '{
        "transitions": {
            "BROUILLON":  ["SOUMIS"],
            "SOUMIS":     ["INCOMPLET", "EN_REVUE"],
            "INCOMPLET":  ["SOUMIS"],
            "EN_REVUE":   ["VALIDE", "REJETE", "INCOMPLET"],
            "VALIDE":     ["CLOTURE"],
            "REJETE":     ["CLOTURE", "SOUMIS"],
            "CLOTURE":    []
        },
        "superviseurRequired": ["VALIDE", "REJETE"]
    }',
    checklist       JSONB DEFAULT '[]',
    actif           BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id)
);

CREATE TABLE dossiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    reference       VARCHAR(30) UNIQUE NOT NULL,
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    type_service_id UUID NOT NULL REFERENCES types_service(id),
    statut          VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
    agent_id        UUID REFERENCES users(id),
    priorite        VARCHAR(20) DEFAULT 'NORMALE',
    date_soumission TIMESTAMPTZ,
    date_echeance   TIMESTAMPTZ,
    motif_rejet     TEXT,
    notes_internes  TEXT,
    metadata        JSONB DEFAULT '{}',
    actif           BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id)
);

CREATE TABLE dossier_historique (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id  UUID NOT NULL REFERENCES dossiers(id),
    statut_de   VARCHAR(30),
    statut_vers VARCHAR(30) NOT NULL,
    commentaire TEXT,
    user_id     UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE dossier_commentaires (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id UUID NOT NULL REFERENCES dossiers(id),
    auteur_id  UUID REFERENCES users(id),
    contenu    TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_dossiers_org_statut ON dossiers(organisation_id, statut) WHERE actif = true;
CREATE INDEX idx_dossiers_usager     ON dossiers(usager_id);
CREATE INDEX idx_dossiers_agent      ON dossiers(agent_id);
CREATE INDEX idx_dossiers_echeance   ON dossiers(date_echeance) WHERE statut NOT IN ('CLOTURE','REJETE');

INSERT INTO types_service (organisation_id, code, libelle, delai_jours, tarif_defaut, checklist)
SELECT o.id, 'PASSEPORT', 'Passeport ordinaire', 30, 86.00,
  '[{"code":"FORMULAIRE","libelle":"Formulaire de demande signé"},
    {"code":"PHOTO","libelle":"2 photos d''identité récentes"},
    {"code":"ANCIEN_PASSEPORT","libelle":"Ancien passeport (si renouvellement)"},
    {"code":"JUSTIFICATIF_DOMICILE","libelle":"Justificatif de domicile < 3 mois"},
    {"code":"ACTE_NAISSANCE","libelle":"Acte de naissance"}]'
FROM organisations o WHERE o.code = 'GNB-FR';

INSERT INTO types_service (organisation_id, code, libelle, delai_jours, tarif_defaut, checklist)
SELECT o.id, 'ETAT_CIVIL', 'Acte d''état civil', 15, 25.00,
  '[{"code":"PIECE_IDENTITE","libelle":"Pièce d''identité valide"},
    {"code":"FORMULAIRE","libelle":"Formulaire de demande"}]'
FROM organisations o WHERE o.code = 'GNB-FR';

INSERT INTO types_service (organisation_id, code, libelle, delai_jours, tarif_defaut)
SELECT o.id, 'CARTE_CONSULAIRE', 'Carte consulaire', 20, 15.00
FROM organisations o WHERE o.code = 'GNB-FR';

INSERT INTO types_service (organisation_id, code, libelle, delai_jours, tarif_defaut)
SELECT o.id, 'LEGALISATION', 'Légalisation de document', 5, 30.00
FROM organisations o WHERE o.code = 'GNB-FR';

INSERT INTO types_service (organisation_id, code, libelle, delai_jours, tarif_defaut)
SELECT o.id, 'PROCURATION', 'Procuration consulaire', 3, 20.00
FROM organisations o WHERE o.code = 'GNB-FR';
