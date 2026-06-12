CREATE TABLE config_creneaux (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    type_service_id UUID REFERENCES types_service(id),
    jour_semaine    INTEGER NOT NULL,
    heure_debut     TIME NOT NULL,
    heure_fin       TIME NOT NULL,
    duree_minutes   INTEGER DEFAULT 30,
    quota_max       INTEGER DEFAULT 10,
    actif           BOOLEAN DEFAULT true
);

CREATE TABLE jours_fermeture (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    date_fermeture  DATE NOT NULL,
    motif           VARCHAR(200),
    UNIQUE (organisation_id, date_fermeture)
);

CREATE TABLE rendezvous (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    usager_id       UUID NOT NULL REFERENCES usagers(id),
    dossier_id      UUID REFERENCES dossiers(id),
    type_service_id UUID REFERENCES types_service(id),
    agent_id        UUID REFERENCES users(id),
    date_heure      TIMESTAMPTZ NOT NULL,
    duree_minutes   INTEGER DEFAULT 30,
    statut          VARCHAR(20) DEFAULT 'CONFIRME',
    motif           TEXT,
    notes           TEXT,
    rappel_envoye   BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_rdv_org_date    ON rendezvous(organisation_id, date_heure);
CREATE INDEX idx_rdv_agent_date  ON rendezvous(agent_id, date_heure);
CREATE UNIQUE INDEX idx_rdv_no_overlap ON rendezvous(agent_id, date_heure)
    WHERE statut NOT IN ('ANNULE', 'REPORTE');

INSERT INTO config_creneaux (organisation_id, jour_semaine, heure_debut, heure_fin, duree_minutes, quota_max)
SELECT o.id, jour, '09:00', '12:00', 30, 8 FROM organisations o, generate_series(1,5) jour
WHERE o.code = 'GNB-FR';

INSERT INTO config_creneaux (organisation_id, jour_semaine, heure_debut, heure_fin, duree_minutes, quota_max)
SELECT o.id, jour, '14:00', '17:00', 30, 8 FROM organisations o, generate_series(1,5) jour
WHERE o.code = 'GNB-FR';
