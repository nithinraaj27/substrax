CREATE TABLE notification_event (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_user
    ON notification_event(user_id);

CREATE UNIQUE INDEX idx_notification_event
    ON notification_event(event_id);
