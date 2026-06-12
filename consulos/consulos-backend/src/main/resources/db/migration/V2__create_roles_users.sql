CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50) UNIQUE NOT NULL,
    libelle     VARCHAR(100) NOT NULL,
    permissions JSONB DEFAULT '[]'
);

INSERT INTO roles (code, libelle) VALUES
('ROLE_ADMIN',       'Administrateur système'),
('ROLE_SUPERVISEUR', 'Superviseur de service'),
('ROLE_AGENT',       'Agent métier'),
('ROLE_CAISSIER',    'Agent de caisse'),
('ROLE_ACCUEIL',     'Agent d''accueil');

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id    UUID NOT NULL REFERENCES organisations(id),
    email              VARCHAR(255) UNIQUE NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    nom                VARCHAR(100) NOT NULL,
    prenom             VARCHAR(100) NOT NULL,
    role_id            UUID NOT NULL REFERENCES roles(id),
    actif              BOOLEAN DEFAULT true,
    derniere_connexion TIMESTAMPTZ,
    created_at         TIMESTAMPTZ DEFAULT NOW(),
    updated_at         TIMESTAMPTZ DEFAULT NOW(),
    created_by         UUID
);

INSERT INTO users (organisation_id, email, password_hash, nom, prenom, role_id)
SELECT o.id,
       'admin@consulos.local',
       '$2b$12$BXEkQ5aHPdeGaGSb.ScvtOUl5iwZ6iB1a45XZTHHPncr79VEG5Yxm',
       'Admin',
       'Système',
       r.id
FROM organisations o, roles r
WHERE o.code = 'GNB-FR' AND r.code = 'ROLE_ADMIN';
