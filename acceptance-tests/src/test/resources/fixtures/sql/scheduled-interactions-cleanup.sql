-- Per-scenario cleanup for @backend-seed API scenarios.
-- Only the transactional scheduled_interactions table is cleared, so each scenario starts with no
-- pending interactions. The seed tables (users, employees, companies, projects, interactions,
-- tasks) are deliberately preserved — these scenarios verify the backend's own startup seed data.
TRUNCATE TABLE scheduled_interactions CASCADE;
