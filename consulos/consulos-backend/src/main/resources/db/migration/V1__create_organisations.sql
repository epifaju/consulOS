CREATE TABLE organisations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(20) UNIQUE NOT NULL,
    nom           VARCHAR(200) NOT NULL,
    pays_code     VARCHAR(3) NOT NULL,
    langue_defaut VARCHAR(5) DEFAULT 'fr',
    devise_defaut VARCHAR(3) DEFAULT 'EUR',
    actif         BOOLEAN DEFAULT true,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO organisations (code, nom, pays_code) VALUES
('GNB-FR', 'Consulat Général de Guinée-Bissau en France', 'GNB'),
('SEN-FR', 'Consulat Général du Sénégal en France', 'SEN');
