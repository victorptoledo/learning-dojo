-- Widen the key and everything that references it. On a big table this rewrites it under an
-- ACCESS EXCLUSIVE lock: in production you would add a bigint column, backfill it in batches
-- and switch, but the end state is the same.
ALTER TABLE order_items ALTER COLUMN order_id TYPE bigint;
ALTER TABLE orders ALTER COLUMN id TYPE bigint;

-- A serial's sequence is a separate object: it keeps its integer limit until told otherwise.
ALTER SEQUENCE orders_id_seq AS bigint;
