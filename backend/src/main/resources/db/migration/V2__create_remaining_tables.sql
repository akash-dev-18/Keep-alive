CREATE TABLE monitor_checks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id       UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status           VARCHAR(10) NOT NULL,
    response_time_ms INT,
    http_status_code INT,
    error_message    TEXT,
    checked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checks_monitor_checked ON monitor_checks(monitor_id, checked_at DESC);

CREATE TABLE incidents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id       UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status           VARCHAR(20) NOT NULL DEFAULT 'open',
    started_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at      TIMESTAMPTZ,
    duration_seconds INT,
    cause            TEXT
);

CREATE INDEX idx_incidents_monitor_id ON incidents(monitor_id);
CREATE INDEX idx_incidents_open ON incidents(status) WHERE status = 'open';

CREATE TABLE alert_logs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id       UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    incident_id      UUID REFERENCES incidents(id),
    alert_type       VARCHAR(30) NOT NULL,
    channel          VARCHAR(20) NOT NULL,
    sent_to          VARCHAR(255) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    error_message    TEXT,
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_logs_monitor_id ON alert_logs(monitor_id);

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monitor_id  UUID REFERENCES monitors(id) ON DELETE SET NULL,
    incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;

CREATE TABLE status_pages (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    slug           VARCHAR(100) UNIQUE NOT NULL,
    monitor_ids    UUID[] NOT NULL DEFAULT '{}',
    is_public      BOOLEAN NOT NULL DEFAULT TRUE,
    custom_domain  VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE monitors ADD COLUMN ssl_days_remaining INT;
ALTER TABLE monitors ADD COLUMN ssl_expires_at TIMESTAMPTZ;
ALTER TABLE monitors ADD COLUMN last_pinged_at TIMESTAMPTZ;

CREATE INDEX idx_monitors_next_check_at_active ON monitors(next_check_at) WHERE is_active = TRUE;
CREATE INDEX idx_monitors_ping_key ON monitors(ping_key) WHERE ping_key IS NOT NULL;