ALTER TABLE monitors ADD COLUMN consecutive_failures INT NOT NULL DEFAULT 0;
ALTER TABLE monitors ADD COLUMN alert_after_failures INT NOT NULL DEFAULT 1;
ALTER TABLE monitors ADD COLUMN expected_keyword VARCHAR(512);
ALTER TABLE monitors ADD COLUMN basic_auth_username VARCHAR(255);
ALTER TABLE monitors ADD COLUMN basic_auth_password_enc TEXT;
ALTER TABLE monitors ADD COLUMN bearer_token_enc TEXT;
ALTER TABLE monitors ADD COLUMN ssl_issuer VARCHAR(512);
ALTER TABLE monitors ADD COLUMN expected_next_heartbeat_at TIMESTAMPTZ;
ALTER TABLE monitors ADD COLUMN last_response_time_ms INT;

CREATE INDEX idx_monitors_status ON monitors(status);
CREATE INDEX idx_monitors_user_status ON monitors(user_id, status);

CREATE TABLE ssl_alert_sent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    threshold_days INT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(monitor_id, threshold_days)
);

CREATE INDEX idx_ssl_alert_sent_monitor ON ssl_alert_sent(monitor_id);

CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_on_down BOOLEAN NOT NULL DEFAULT TRUE,
    email_on_up BOOLEAN NOT NULL DEFAULT TRUE,
    email_on_ssl_expiry BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE status_pages ADD COLUMN description TEXT;
ALTER TABLE status_pages ADD COLUMN logo_url VARCHAR(2048);
ALTER TABLE status_pages ADD COLUMN primary_color VARCHAR(7) DEFAULT '#06b6d4';
