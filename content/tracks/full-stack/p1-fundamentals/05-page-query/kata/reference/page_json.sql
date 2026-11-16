WITH r AS (
    SELECT * FROM page_request
),
-- One row more than the page: if it exists, another page follows.
candidates AS (
    SELECT o.id, o.placed_at,
           row_number() OVER (ORDER BY o.placed_at DESC, o.id DESC) AS position
    FROM r
    JOIN orders AS o ON o.customer_id = r.customer_id
    WHERE r.after_id IS NULL
       OR (o.placed_at, o.id) < (r.after_placed_at, r.after_id)
    ORDER BY o.placed_at DESC, o.id DESC
    LIMIT (SELECT page_size + 1 FROM r)
),
page AS (
    SELECT c.id, c.placed_at, c.position,
           to_char(c.placed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS placed_at_utc
    FROM candidates AS c
    WHERE c.position <= (SELECT page_size FROM r)
),
shaped AS (
    SELECT p.position,
           jsonb_build_object(
               'id', p.id,
               'placedAt', p.placed_at_utc,
               'totalCents', sum(i.quantity * i.unit_price_cents),
               'items', jsonb_agg(
                   jsonb_build_object('product', pr.name, 'quantity', i.quantity,
                                      'unitPriceCents', i.unit_price_cents)
                   ORDER BY pr.name)
           ) AS doc
    FROM page AS p
    JOIN order_items AS i ON i.order_id = p.id
    JOIN products AS pr ON pr.id = i.product_id
    GROUP BY p.id, p.position, p.placed_at_utc
)
SELECT jsonb_build_object(
    'customer', (SELECT jsonb_build_object('id', c.id, 'name', c.name)
                 FROM customers AS c JOIN r ON r.customer_id = c.id),
    'orders', coalesce((SELECT jsonb_agg(doc ORDER BY position) FROM shaped), '[]'::jsonb),
    'nextCursor', CASE
        WHEN (SELECT count(*) FROM candidates) > (SELECT page_size FROM r)
        THEN (SELECT jsonb_build_object('placedAt', placed_at_utc, 'id', id)
              FROM page ORDER BY position DESC LIMIT 1)
        ELSE 'null'::jsonb
    END
) AS page;
