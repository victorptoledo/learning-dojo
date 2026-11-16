-- Apply a CRM snapshot to the type 2 customer dimension.
-- name is type 1: corrected in every version, no new version.
UPDATE dim_customer AS d
SET name = s.name
FROM customer_snapshot AS s
WHERE d.customer_id = s.customer_id
  AND d.name IS DISTINCT FROM s.name;

-- city and segment are type 2: close the current version and open a new one.
-- IS DISTINCT FROM treats NULL as a value, so NULL -> 'enterprise' is a change.
WITH closed AS (
    UPDATE dim_customer AS d
    SET valid_to = s.snapshot_date,
        is_current = false
    FROM customer_snapshot AS s
    WHERE d.customer_id = s.customer_id
      AND d.is_current
      AND (d.city, d.segment) IS DISTINCT FROM (s.city, s.segment)
    RETURNING s.customer_id, s.name, s.city, s.segment, s.snapshot_date
)
INSERT INTO dim_customer (customer_id, name, city, segment, valid_from, valid_to, is_current)
SELECT customer_id, name, city, segment, snapshot_date, DATE '9999-12-31', true
FROM closed;

-- Customers the dimension has never seen.
INSERT INTO dim_customer (customer_id, name, city, segment, valid_from, valid_to, is_current)
SELECT s.customer_id, s.name, s.city, s.segment, s.snapshot_date, DATE '9999-12-31', true
FROM customer_snapshot AS s
WHERE NOT EXISTS (SELECT 1 FROM dim_customer AS d WHERE d.customer_id = s.customer_id);
