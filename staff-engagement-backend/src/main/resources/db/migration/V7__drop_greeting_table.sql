-- The greeting feature was scaffolding: the table was never read or written by
-- the application, and the frontend derives its greeting client-side. Dropping
-- the table alongside the removal of the Greeting entity, repository and service.
DROP TABLE IF EXISTS greeting;
