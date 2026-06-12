-- Utilisateurs de démo pour tests E2E (mot de passe : Admin1234!)
INSERT INTO users (organisation_id, email, password_hash, nom, prenom, role_id)
SELECT o.id,
       'agent@consulos.local',
       '$2b$12$BXEkQ5aHPdeGaGSb.ScvtOUl5iwZ6iB1a45XZTHHPncr79VEG5Yxm',
       'Agent',
       'Test',
       r.id
FROM organisations o, roles r
WHERE o.code = 'GNB-FR' AND r.code = 'ROLE_AGENT'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'agent@consulos.local');

INSERT INTO users (organisation_id, email, password_hash, nom, prenom, role_id)
SELECT o.id,
       'superviseur@consulos.local',
       '$2b$12$BXEkQ5aHPdeGaGSb.ScvtOUl5iwZ6iB1a45XZTHHPncr79VEG5Yxm',
       'Superviseur',
       'Test',
       r.id
FROM organisations o, roles r
WHERE o.code = 'GNB-FR' AND r.code = 'ROLE_SUPERVISEUR'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'superviseur@consulos.local');

INSERT INTO users (organisation_id, email, password_hash, nom, prenom, role_id)
SELECT o.id,
       'caissier@consulos.local',
       '$2b$12$BXEkQ5aHPdeGaGSb.ScvtOUl5iwZ6iB1a45XZTHHPncr79VEG5Yxm',
       'Caissier',
       'Test',
       r.id
FROM organisations o, roles r
WHERE o.code = 'GNB-FR' AND r.code = 'ROLE_CAISSIER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'caissier@consulos.local');
