-- Données de démonstration pour démo client et tests E2E (idempotent)

INSERT INTO usagers (organisation_id, numero, nom, prenom, date_naissance, nationalite, type_usager, email, telephone)
SELECT o.id,
       'GNB-2024-00002',
       'Camara',
       'Aminata',
       '1992-03-15'::DATE,
       'GNB',
       'DIASPORA',
       'aminata.camara@email.com',
       '+33698765432'
FROM organisations o
WHERE o.code = 'GNB-FR'
  AND NOT EXISTS (SELECT 1 FROM usagers u WHERE u.numero = 'GNB-2024-00002');

INSERT INTO dossiers (organisation_id, reference, usager_id, type_service_id, statut, agent_id, priorite)
SELECT o.id,
       'DOS-2026-00001',
       u.id,
       ts.id,
       'BROUILLON',
       agent.id,
       'NORMALE'
FROM organisations o
JOIN usagers u ON u.organisation_id = o.id AND u.numero = 'GNB-2024-00001'
JOIN types_service ts ON ts.organisation_id = o.id AND ts.code = 'PASSEPORT'
LEFT JOIN users agent ON agent.email = 'agent@consulos.local'
WHERE o.code = 'GNB-FR'
  AND NOT EXISTS (SELECT 1 FROM dossiers d WHERE d.reference = 'DOS-2026-00001');

INSERT INTO dossiers (organisation_id, reference, usager_id, type_service_id, statut, agent_id, priorite, date_soumission)
SELECT o.id,
       'DOS-2026-00002',
       u.id,
       ts.id,
       'SOUMIS',
       agent.id,
       'NORMALE',
       NOW()
FROM organisations o
JOIN usagers u ON u.organisation_id = o.id AND u.numero = 'GNB-2024-00002'
JOIN types_service ts ON ts.organisation_id = o.id AND ts.code = 'ETAT_CIVIL'
LEFT JOIN users agent ON agent.email = 'agent@consulos.local'
WHERE o.code = 'GNB-FR'
  AND NOT EXISTS (SELECT 1 FROM dossiers d WHERE d.reference = 'DOS-2026-00002');

INSERT INTO dossier_historique (dossier_id, statut_de, statut_vers, commentaire, user_id)
SELECT d.id,
       'BROUILLON',
       'SOUMIS',
       'Dossier soumis pour démonstration',
       agent.id
FROM dossiers d
JOIN organisations o ON o.id = d.organisation_id
LEFT JOIN users agent ON agent.email = 'agent@consulos.local'
WHERE o.code = 'GNB-FR'
  AND d.reference = 'DOS-2026-00002'
  AND NOT EXISTS (
      SELECT 1 FROM dossier_historique h
      WHERE h.dossier_id = d.id AND h.statut_vers = 'SOUMIS'
  );

INSERT INTO rendezvous (organisation_id, usager_id, dossier_id, type_service_id, agent_id, date_heure, duree_minutes, statut, motif)
SELECT o.id,
       u.id,
       d.id,
       ts.id,
       agent.id,
       (CURRENT_DATE + INTERVAL '1 day')::TIMESTAMPTZ + TIME '10:00',
       30,
       'CONFIRME',
       'Renouvellement passeport — démo'
FROM organisations o
JOIN usagers u ON u.organisation_id = o.id AND u.numero = 'GNB-2024-00001'
JOIN dossiers d ON d.usager_id = u.id AND d.reference = 'DOS-2026-00001'
JOIN types_service ts ON ts.organisation_id = o.id AND ts.code = 'PASSEPORT'
LEFT JOIN users agent ON agent.email = 'agent@consulos.local'
WHERE o.code = 'GNB-FR'
  AND NOT EXISTS (
      SELECT 1 FROM rendezvous r
      WHERE r.usager_id = u.id
        AND r.motif = 'Renouvellement passeport — démo'
  );
