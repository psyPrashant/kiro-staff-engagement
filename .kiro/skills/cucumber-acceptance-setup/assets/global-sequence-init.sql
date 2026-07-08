-- Global sequence init: reset id sequences so DEFAULT-generated ids in seed inserts start at
-- 990001 — safely inside the `id > 900000` cleanup range.
--
-- PostgreSQL syntax shown. For other databases:
--   MySQL:      ALTER TABLE <table> AUTO_INCREMENT = 990001;
--   SQL Server: DBCC CHECKIDENT ('<table>', RESEED, 990000);
--
-- Add one line per sequence that acceptance-test inserts rely on. Replace the illustrative lines.

ALTER SEQUENCE order_id_seq    RESTART WITH 990001;
ALTER SEQUENCE customer_id_seq RESTART WITH 990001;
