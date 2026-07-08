-- Global cleanup: remove all acceptance-test data before every scenario.
--
-- Convention: every row created by a test uses an id above the high watermark (900000),
-- so cleanup is a set of DELETEs scoped to `id > 900000`. This keeps real/reference data safe.
--
-- Delete in FOREIGN-KEY-SAFE order: child/leaf tables first, parent tables last. Fill in the
-- statements for your schema. The two lines below are illustrative — replace them.

DELETE FROM order_line WHERE id > 900000;
DELETE FROM orders     WHERE id > 900000;
DELETE FROM customer   WHERE id > 900000;

-- Add one DELETE per table that acceptance tests write to, ordered so no statement violates a
-- foreign key (a table must be emptied before the table it points to).
