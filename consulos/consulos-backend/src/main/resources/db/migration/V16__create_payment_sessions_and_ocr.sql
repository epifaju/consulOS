-- Phase 3 — paiement en ligne (sessions) + résultats OCR documents
CREATE TABLE payment_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id     UUID NOT NULL REFERENCES organisations(id),
    encaissement_id     UUID NOT NULL REFERENCES encaissements(id),
    provider            VARCHAR(30) NOT NULL,
    provider_session_id VARCHAR(100) NOT NULL UNIQUE,
    status              VARCHAR(30) NOT NULL,
    amount              NUMERIC(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'EUR',
    checkout_url        VARCHAR(500),
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_payment_sessions_enc
    ON payment_sessions (encaissement_id, created_at DESC);

CREATE INDEX idx_payment_sessions_org_status
    ON payment_sessions (organisation_id, status);

CREATE TABLE document_ocr_results (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    document_id     UUID NOT NULL REFERENCES documents(id),
    provider        VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    extracted_text  TEXT,
    error_message   TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_document_ocr_document UNIQUE (document_id)
);

CREATE INDEX idx_document_ocr_org
    ON document_ocr_results (organisation_id, created_at DESC);
