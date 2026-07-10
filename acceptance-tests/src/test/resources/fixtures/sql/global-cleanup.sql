-- Global cleanup: TRUNCATE all application tables before each scenario for test isolation.
-- Add new tables here as they are created in the application schema.
-- The greeting table is from the test harness sample; add real domain tables below.
TRUNCATE TABLE greeting CASCADE;
TRUNCATE TABLE tasks CASCADE;
TRUNCATE TABLE interactions CASCADE;
TRUNCATE TABLE projects CASCADE;
TRUNCATE TABLE companies CASCADE;
TRUNCATE TABLE employees CASCADE;
TRUNCATE TABLE users CASCADE;
