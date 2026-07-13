CREATE TABLE scheduled_interactions (
    id                      BIGSERIAL PRIMARY KEY,
    employee_id             BIGINT NOT NULL REFERENCES employees(id),
    scheduled_by_user_id    BIGINT NOT NULL REFERENCES users(id),
    scheduled_date          DATE NOT NULL,
    interaction_type        VARCHAR(50) NOT NULL,
    notes                   TEXT,
    completion_status       VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMP NOT NULL,
    CONSTRAINT chk_si_interaction_type CHECK (interaction_type IN ('CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER')),
    CONSTRAINT chk_si_completion_status CHECK (completion_status IN ('PENDING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_scheduled_interactions_employee_id ON scheduled_interactions(employee_id);
CREATE INDEX idx_scheduled_interactions_scheduled_by_user_id ON scheduled_interactions(scheduled_by_user_id);
CREATE INDEX idx_scheduled_interactions_scheduled_date ON scheduled_interactions(scheduled_date);
