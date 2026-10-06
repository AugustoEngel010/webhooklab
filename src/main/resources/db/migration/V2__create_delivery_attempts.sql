CREATE TABLE delivery_attempts (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT,
    outcome VARCHAR(30) NOT NULL,
    http_status INTEGER,
    CONSTRAINT delivery_attempts_event_fk FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT delivery_attempts_event_unique UNIQUE (event_id),
    CONSTRAINT delivery_attempts_outcome_check CHECK (outcome IN ('STARTED', 'SUCCEEDED', 'HTTP_ERROR', 'TIMEOUT', 'CONNECTION_ERROR'))
);

CREATE INDEX delivery_attempts_started_at_idx ON delivery_attempts (started_at);
