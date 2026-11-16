SELECT o.id,
       o.placed_at,
       sum(i.quantity) AS item_count,
       sum(i.quantity * i.unit_price_cents) AS total_cents
FROM page_request AS r
JOIN orders AS o ON o.customer_id = r.customer_id
JOIN order_items AS i ON i.order_id = o.id
WHERE r.after_id IS NULL
   OR (o.placed_at, o.id) < (r.after_placed_at, r.after_id)
GROUP BY o.id, o.placed_at, r.page_size
ORDER BY o.placed_at DESC, o.id DESC
LIMIT (SELECT page_size FROM page_request);
