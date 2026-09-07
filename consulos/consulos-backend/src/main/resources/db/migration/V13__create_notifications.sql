-- Phase 2 M10 — notifications in-app (WebSocket / STOMP)
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id),
    recipient_user_id UUID NOT NULL REFERENCES users(id),
    type_notification VARCHAR(50) NOT NULL,
    titre           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    lien            VARCHAR(500),
    lu              BOOLEAN NOT NULL DEFAULT false,
    lu_at           TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id)
);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications (organisation_id, recipient_user_id, lu, created_at DESC);

CREATE INDEX idx_notifications_recipient
    ON notifications (organisation_id, recipient_user_id, created_at DESC);
