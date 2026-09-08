-- Phase 3 Vague A — multi-postes mutualisés (réseau + postes + membres)

INSERT INTO roles (code, libelle, permissions)
SELECT 'ROLE_ADMIN_RESEAU',
       'Administrateur réseau multi-postes',
       '["reseau:read","reseau:write"]'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_ADMIN_RESEAU');

CREATE TABLE reseaux (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50) UNIQUE NOT NULL,
    nom         VARCHAR(200) NOT NULL,
    description TEXT,
    actif       BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    created_by  UUID REFERENCES users(id),
    updated_by  UUID REFERENCES users(id)
);

CREATE TABLE reseau_postes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    reseau_id       UUID NOT NULL REFERENCES reseaux(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    CONSTRAINT uq_reseau_poste UNIQUE (reseau_id, organisation_id)
);

CREATE INDEX idx_reseau_postes_reseau ON reseau_postes (reseau_id);
CREATE INDEX idx_reseau_postes_org ON reseau_postes (organisation_id);

CREATE TABLE reseau_membres (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    reseau_id       UUID NOT NULL REFERENCES reseaux(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_reseau     VARCHAR(30) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    CONSTRAINT uq_reseau_membre UNIQUE (reseau_id, user_id),
    CONSTRAINT chk_reseau_membre_role CHECK (role_reseau IN ('ADMIN', 'LECTEUR'))
);

CREATE INDEX idx_reseau_membres_user ON reseau_membres (user_id);
CREATE INDEX idx_reseau_membres_reseau ON reseau_membres (reseau_id);

-- Utilisateur démo réseau (poste d'attache GNB-FR pour JWT organisationId)
INSERT INTO users (organisation_id, email, password_hash, nom, prenom, role_id)
SELECT o.id,
       'reseau@consulos.local',
       '$2b$12$BXEkQ5aHPdeGaGSb.ScvtOUl5iwZ6iB1a45XZTHHPncr79VEG5Yxm',
       'Réseau',
       'Pilotage',
       r.id
FROM organisations o, roles r
WHERE o.code = 'GNB-FR' AND r.code = 'ROLE_ADMIN_RESEAU'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'reseau@consulos.local');

-- Réseau démo Afrique de l'Ouest — Europe
INSERT INTO reseaux (id, code, nom, description, actif)
SELECT 'a1000000-0000-4000-8000-000000000001'::uuid,
       'AO-EU',
       'Afrique de l''Ouest — Europe',
       'Réseau mutualisé de démo (GNB-FR + SEN-FR)',
       true
WHERE NOT EXISTS (SELECT 1 FROM reseaux WHERE code = 'AO-EU');

INSERT INTO reseau_postes (organisation_id, reseau_id)
SELECT o.id, r.id
FROM organisations o
CROSS JOIN reseaux r
WHERE o.code IN ('GNB-FR', 'SEN-FR')
  AND r.code = 'AO-EU'
  AND NOT EXISTS (
      SELECT 1 FROM reseau_postes rp
      WHERE rp.reseau_id = r.id AND rp.organisation_id = o.id
  );

INSERT INTO reseau_membres (organisation_id, reseau_id, user_id, role_reseau)
SELECT u.organisation_id, r.id, u.id, 'ADMIN'
FROM users u
CROSS JOIN reseaux r
WHERE u.email = 'reseau@consulos.local'
  AND r.code = 'AO-EU'
  AND NOT EXISTS (
      SELECT 1 FROM reseau_membres rm
      WHERE rm.reseau_id = r.id AND rm.user_id = u.id
  );
