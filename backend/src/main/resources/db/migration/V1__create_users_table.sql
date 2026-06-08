CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       clerk_user_id VARCHAR(255) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       name VARCHAR(255),

                       plan VARCHAR(20) NOT NULL DEFAULT 'free',

                       stripe_customer_id VARCHAR(255),
                       stripe_subscription_id VARCHAR(255),

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE monitors (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          user_id UUID NOT NULL,

                          name VARCHAR(255) NOT NULL,

                          type VARCHAR(20) NOT NULL,

                          url VARCHAR(2048),

                          ping_key VARCHAR(100) UNIQUE,

                          check_interval_min INT NOT NULL DEFAULT 5,

                          http_method VARCHAR(10) DEFAULT 'GET',

                          expected_status INT DEFAULT 200,

                          timeout_seconds INT NOT NULL DEFAULT 30,

                          status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',

                          last_checked_at TIMESTAMPTZ,

                          next_check_at TIMESTAMPTZ NOT NULL,

                          is_active BOOLEAN NOT NULL DEFAULT TRUE,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_monitor_user
                              FOREIGN KEY(user_id)
                                  REFERENCES users(id)
                                  ON DELETE CASCADE
);

CREATE INDEX idx_monitors_user_id
    ON monitors(user_id);

CREATE INDEX idx_monitors_next_check_at
    ON monitors(next_check_at);