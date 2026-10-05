CREATE TABLE events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT events_event_type_not_blank CHECK (length(btrim(event_type)) > 0),
    CONSTRAINT events_status_check CHECK (status IN ('PENDING', 'SENDING', 'DELIVERED', 'FAILED'))
);

CREATE INDEX events_created_at_id_idx ON events (created_at DESC, id DESC);
