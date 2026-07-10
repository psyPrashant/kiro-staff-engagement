-- employee360_seed.sql
-- Seed data for Employee 360 View acceptance tests.
-- Supports scenarios: full data (employee 1), empty interactions (employee 2), overdue tasks.

-- ============================================================
-- 1. Users (for conducted_by / logged_by / assigned_to references and login)
-- ============================================================
INSERT INTO users (id, name, email, password_hash, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Admin User', 'admin@psybergate.co.za', '$2a$10$QqECXUNPAZ3.G.3xxJzrpuT3Q/3v3UF9kbJCOB6N6JQIsXh.ubP8S', '2024-01-01 00:00:00'),
    (2, 'Jane Manager', 'jane.manager@psybergate.co.za', '$2a$10$QqECXUNPAZ3.G.3xxJzrpuT3Q/3v3UF9kbJCOB6N6JQIsXh.ubP8S', '2024-01-01 00:00:00'),
    (3, 'Bob Mentor', 'bob.mentor@psybergate.co.za', '$2a$10$QqECXUNPAZ3.G.3xxJzrpuT3Q/3v3UF9kbJCOB6N6JQIsXh.ubP8S', '2024-01-01 00:00:00');

-- ============================================================
-- 2. Employees (manager first due to self-referencing FK)
-- ============================================================
-- Employee 1: has full data (interactions, tasks, project context)
INSERT INTO employees (id, name, email, manager_id, job_title, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'John Developer', 'john.developer@psybergate.co.za', NULL, 'Software Engineer', '2024-01-15 09:00:00');

-- Now update employee 1's manager to reference a manager employee
-- First insert the manager as employee 3 (we'll use employee 3 as the manager)
INSERT INTO employees (id, name, email, manager_id, job_title, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (3, 'Jane Manager', 'jane.mgr@psybergate.co.za', NULL, 'Engineering Manager', '2024-01-01 09:00:00');

-- Set employee 1's manager
UPDATE employees SET manager_id = 3 WHERE id = 1;

-- Employee 2: has NO interactions (empty state scenario)
INSERT INTO employees (id, name, email, manager_id, job_title, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (2, 'Empty Employee', 'empty.employee@psybergate.co.za', NULL, 'Junior Developer', '2024-02-01 09:00:00');

-- ============================================================
-- 3. Companies
-- ============================================================
INSERT INTO companies (id, name, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Acme Corp', '2024-01-01 00:00:00');

-- ============================================================
-- 4. Projects
-- ============================================================
INSERT INTO projects (id, name, company_id, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Alpha Platform', 1, '2024-01-10 00:00:00');

-- ============================================================
-- 5. Interactions for employee 1 (various types and dates for ordering test)
-- ============================================================
-- Interaction 1: CHECK_IN, most recent, WITH project context
INSERT INTO interactions (id, employee_id, conducted_by_user_id, logged_by_user_id, project_id, type, notes, occurred_at, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 2, 1, 1, 'CHECK_IN', 'Discussed project progress and upcoming sprint goals. Employee is performing well and meeting deadlines consistently.', '2024-12-15 10:00:00', '2024-12-15 10:30:00');

-- Interaction 2: MENTORING, older date, WITHOUT project context
INSERT INTO interactions (id, employee_id, conducted_by_user_id, logged_by_user_id, project_id, type, notes, occurred_at, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (2, 1, 3, 1, NULL, 'MENTORING', 'Mentoring session on system design patterns. Covered SOLID principles and domain-driven design basics.', '2024-11-20 14:00:00', '2024-11-20 14:30:00');

-- Interaction 3: CATCH_UP, oldest, WITH project context
INSERT INTO interactions (id, employee_id, conducted_by_user_id, logged_by_user_id, project_id, type, notes, occurred_at, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (3, 1, 2, 2, 1, 'CATCH_UP', 'General catch-up on work-life balance and career aspirations. Employee expressed interest in leading a small team in the next quarter.', '2024-10-05 09:00:00', '2024-10-05 09:30:00');

-- ============================================================
-- 6. Tasks for employee 1's interactions
-- ============================================================
-- Task 1: OPEN, past due date (OVERDUE)
INSERT INTO tasks (id, interaction_id, title, description, status, due_date, assigned_user_id, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 'Update project documentation', 'Update the API documentation for the new endpoints', 'OPEN', '2024-12-01', 1, '2024-12-15 10:30:00');

-- Task 2: OPEN, future due date (NOT overdue)
INSERT INTO tasks (id, interaction_id, title, description, status, due_date, assigned_user_id, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (2, 1, 'Complete design review', 'Review the system design document and provide feedback', 'OPEN', '2099-12-31', 1, '2024-12-15 10:30:00');

-- Task 3: OPEN, NULL due date (should appear last, NOT overdue)
INSERT INTO tasks (id, interaction_id, title, description, status, due_date, assigned_user_id, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (3, 2, 'Read DDD book chapter 5', 'Study domain-driven design chapter on aggregates', 'OPEN', NULL, 1, '2024-11-20 14:30:00');

-- Task 4: DONE (should NOT appear in the Employee 360 open tasks view)
INSERT INTO tasks (id, interaction_id, title, description, status, due_date, assigned_user_id, created_at) OVERRIDING SYSTEM VALUE
VALUES
    (4, 3, 'Prepare career development plan', 'Draft a career development plan for next review cycle', 'DONE', '2024-11-01', 1, '2024-10-05 09:30:00');

-- ============================================================
-- 7. Reset sequences to avoid conflicts with future inserts
-- ============================================================
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('employees_id_seq', (SELECT MAX(id) FROM employees));
SELECT setval('companies_id_seq', (SELECT MAX(id) FROM companies));
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));
SELECT setval('interactions_id_seq', (SELECT MAX(id) FROM interactions));
SELECT setval('tasks_id_seq', (SELECT MAX(id) FROM tasks));
