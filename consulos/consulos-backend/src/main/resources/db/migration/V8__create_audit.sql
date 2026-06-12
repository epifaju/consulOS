CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID REFERENCES organisations(id),
    user_id         UUID REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entite_type     VARCHAR(100),
    entite_id       UUID,
    ancien_etat     JSONB,
    nouvel_etat     JSONB,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_org_date ON audit_logs(organisation_id, created_at DESC);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entite ON audit_logs(entite_type, entite_id);

CREATE MATERIALIZED VIEW dashboard_daily_stats AS
SELECT
    d.organisation_id,
    DATE(d.created_at) AS date_stat,
    ts.code AS type_service,
    d.statut,
    COUNT(*) AS nb_dossiers,
    AVG(EXTRACT(EPOCH FROM (d.updated_at - d.date_soumission)) / 3600) AS delai_moyen_heures
FROM dossiers d
JOIN types_service ts ON ts.id = d.type_service_id
WHERE d.actif = true
GROUP BY d.organisation_id, DATE(d.created_at), ts.code, d.statut;

CREATE UNIQUE INDEX idx_dashboard_daily_stats_unique
    ON dashboard_daily_stats(organisation_id, date_stat, type_service, statut);
