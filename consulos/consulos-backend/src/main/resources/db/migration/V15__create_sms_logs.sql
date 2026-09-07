-- Phase 2 3.4 — journal des SMS RDV (optionnel)
CREATE TABLE sms_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    recipient       VARCHAR(30) NOT NULL,
    message_body    TEXT NOT NULL,
    purpose         VARCHAR(40) NOT NULL,
    provider        VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    provider_ref    VARCHAR(100),
    error_message   TEXT,
    related_rdv_id  UUID REFERENCES rendezvous(id),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sms_logs_org_created
    ON sms_logs (organisation_id, created_at DESC);

CREATE INDEX idx_sms_logs_rdv
    ON sms_logs (related_rdv_id)
    WHERE related_rdv_id IS NOT NULL;
