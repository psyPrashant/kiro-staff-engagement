-- V2__create_domain_tables.sql
-- Creates all domain tables for the Staff Engagement platform.

-- 1. Users table
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL
);

-- 2. Employees table (self-referencing manager)
CREATE TABLE employees (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    manager_id  BIGINT REFERENCES employees(id),
    job_title   VARCHAR(255),
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_employees_manager_id ON employees(manager_id);

-- 3. Companies table
CREATE TABLE companies (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

-- 4. Projects table
CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    company_id  BIGINT NOT NULL REFERENCES companies(id),
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_projects_company_id ON projects(company_id);

-- 5. Interactions table
CREATE TABLE interactions (
    id                      BIGSERIAL PRIMARY KEY,
    employee_id             BIGINT NOT NULL REFERENCES employees(id),
    conducted_by_user_id    BIGINT NOT NULL REFERENCES users(id),
    logged_by_user_id       BIGINT NOT NULL REFERENCES users(id),
    project_id              BIGINT REFERENCES projects(id),
    type                    VARCHAR(50) NOT NULL,
    notes                   TEXT NOT NULL,
    occurred_at             TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    CONSTRAINT chk_interaction_type CHECK (type IN ('CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER'))
);

CREATE INDEX idx_interactions_employee_id ON interactions(employee_id);
CREATE INDEX idx_interactions_conducted_by_user_id ON interactions(conducted_by_user_id);
CREATE INDEX idx_interactions_logged_by_user_id ON interactions(logged_by_user_id);
CREATE INDEX idx_interactions_project_id ON interactions(project_id);

-- 6. Tasks table
CREATE TABLE tasks (
    id               BIGSERIAL PRIMARY KEY,
    interaction_id   BIGINT REFERENCES interactions(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    status           VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    due_date         DATE,
    assigned_user_id BIGINT REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL,
    CONSTRAINT chk_task_status CHECK (status IN ('OPEN', 'DONE'))
);

CREATE INDEX idx_tasks_interaction_id ON tasks(interaction_id);
CREATE INDEX idx_tasks_assigned_user_id ON tasks(assigned_user_id);
