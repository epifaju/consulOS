-- Index de performance pour les listes et recherches fréquentes

CREATE INDEX IF NOT EXISTS idx_usagers_org_actif_created
    ON usagers(organisation_id, actif, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_usagers_org_nom_trgm
    ON usagers(organisation_id, nom, prenom)
    WHERE actif = true;

CREATE INDEX IF NOT EXISTS idx_dossiers_org_actif_created
    ON dossiers(organisation_id, actif, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dossiers_org_statut
    ON dossiers(organisation_id, statut)
    WHERE actif = true;

CREATE INDEX IF NOT EXISTS idx_dossiers_org_echeance
    ON dossiers(organisation_id, date_echeance)
    WHERE actif = true AND statut NOT IN ('CLOTURE', 'REJETE');

CREATE INDEX IF NOT EXISTS idx_dossiers_agent_statut
    ON dossiers(agent_id, statut)
    WHERE actif = true AND agent_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rendezvous_org_date
    ON rendezvous(organisation_id, date_heure);

CREATE INDEX IF NOT EXISTS idx_encaissements_org_created
    ON encaissements(organisation_id, created_at DESC);
