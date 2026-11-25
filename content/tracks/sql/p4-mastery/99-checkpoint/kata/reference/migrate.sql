-- Money is exact: convert through numeric and round to cents.
ALTER TABLE plans
    ALTER COLUMN monthly_fee TYPE numeric(10, 2) USING round(monthly_fee::numeric, 2);

-- A list of ids in a column becomes a junction table the database can check.
CREATE TABLE subscriptions (
    customer_id integer NOT NULL REFERENCES customers (id),
    plan_id     integer NOT NULL REFERENCES plans (id),
    PRIMARY KEY (customer_id, plan_id)
);

-- Split and trim the pieces, and join them to plans as text: empty pieces and deleted plans
-- find no match (and no cast can fail on them), and DISTINCT keeps a repeated plan once.
INSERT INTO subscriptions (customer_id, plan_id)
SELECT DISTINCT c.id, p.id
FROM customers c
CROSS JOIN LATERAL unnest(string_to_array(c.plan_ids, ',')) AS piece
JOIN plans p ON p.id::text = trim(piece);

ALTER TABLE customers DROP COLUMN plan_ids;
