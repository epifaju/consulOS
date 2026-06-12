CREATE SEQUENCE usager_seq START 1;

CREATE TABLE usagers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    numero          VARCHAR(30) UNIQUE,
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE,
    lieu_naissance  VARCHAR(200),
    nationalite     VARCHAR(3),
    type_usager     VARCHAR(30) DEFAULT 'DIASPORA',
    telephone       VARCHAR(30),
    email           VARCHAR(255),
    adresse         TEXT,
    numero_document VARCHAR(50),
    type_document   VARCHAR(30),
    metadata        JSONB DEFAULT '{}',
    actif           BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id)
);

CREATE INDEX idx_usagers_org ON usagers(organisation_id) WHERE actif = true;
CREATE INDEX idx_usagers_fulltext ON usagers
  USING GIN (to_tsvector('french',
    coalesce(nom,'') || ' ' ||
    coalesce(prenom,'') || ' ' ||
    coalesce(numero_document,'') || ' ' ||
    coalesce(email,'') || ' ' ||
    coalesce(telephone,'')
  ));

INSERT INTO usagers (organisation_id, numero, nom, prenom, nationalite, type_usager, email, telephone)
SELECT o.id, 'GNB-2024-00001', 'Camara', 'Mamadou', 'GNB', 'DIASPORA',
       'mamadou.camara@email.com', '+33612345678'
FROM organisations o WHERE o.code = 'GNB-FR';
