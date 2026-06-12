CREATE SEQUENCE encaissement_seq START 1;

CREATE TABLE tarifs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    type_service_id UUID REFERENCES types_service(id),
    libelle         VARCHAR(200) NOT NULL,
    montant         DECIMAL(10,2) NOT NULL,
    devise          VARCHAR(3) DEFAULT 'EUR',
    actif           BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE encaissements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    dossier_id      UUID REFERENCES dossiers(id),
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    numero_recu     VARCHAR(30) UNIQUE NOT NULL,
    montant_total   DECIMAL(10,2) NOT NULL,
    montant_percu   DECIMAL(10,2) DEFAULT 0,
    devise          VARCHAR(3) DEFAULT 'EUR',
    mode_paiement   VARCHAR(30),
    statut          VARCHAR(20) DEFAULT 'DU',
    date_paiement   TIMESTAMPTZ,
    caissier_id     UUID REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_enc_org_date   ON encaissements(organisation_id, created_at DESC);
CREATE INDEX idx_enc_caissier   ON encaissements(caissier_id);

INSERT INTO tarifs (organisation_id, type_service_id, libelle, montant)
SELECT o.id, ts.id, ts.libelle || ' — tarif consulaire', ts.tarif_defaut
FROM organisations o
JOIN types_service ts ON ts.organisation_id = o.id
WHERE o.code = 'GNB-FR' AND ts.tarif_defaut IS NOT NULL;
