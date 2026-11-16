SELECT dojo.query_eq(
    'the second page starts strictly after the cursor',
    'order_page.sql',
    $$VALUES (2, TIMESTAMPTZ '2026-01-08 09:30:00+00', 1, 800),
             (1, TIMESTAMPTZ '2026-01-05 10:00:00+00', 3, 2100)$$,
    ordered => true,
    setup => $$UPDATE page_request SET after_placed_at = '2026-01-12 18:00:00+00', after_id = 4$$
);

SELECT dojo.query_eq(
    'a cursor between two orders placed at the same time keeps the other one',
    'order_page.sql',
    $$VALUES (4, TIMESTAMPTZ '2026-01-12 18:00:00+00', 3, 750),
             (2, TIMESTAMPTZ '2026-01-08 09:30:00+00', 1, 800)$$,
    ordered => true,
    setup => $$UPDATE page_request SET page_size = 2, after_placed_at = '2026-01-12 18:00:00+00', after_id = 5$$
);

SELECT dojo.query_eq(
    'only the requested customer, and the page size is respected',
    'order_page.sql',
    $$VALUES (3, TIMESTAMPTZ '2026-01-09 12:00:00+00', 3, 450)$$,
    ordered => true,
    setup => $$UPDATE page_request SET customer_id = 2, page_size = 10$$
);

SELECT dojo.query_eq(
    'the first page as the JSON the screen renders',
    'page_json.sql',
    $$VALUES ('{"customer": {"id": 1, "name": "Ada"},
               "orders": [
                 {"id": 6, "placedAt": "2026-01-15T08:15:00Z", "totalCents": 2950,
                  "items": [{"product": "Mug", "quantity": 1, "unitPriceCents": 1200},
                            {"product": "Notebook", "quantity": 2, "unitPriceCents": 800},
                            {"product": "Pen", "quantity": 1, "unitPriceCents": 150}]},
                 {"id": 5, "placedAt": "2026-01-12T18:00:00Z", "totalCents": 2200,
                  "items": [{"product": "Mug", "quantity": 2, "unitPriceCents": 1100}]},
                 {"id": 4, "placedAt": "2026-01-12T18:00:00Z", "totalCents": 750,
                  "items": [{"product": "Pen", "quantity": 2, "unitPriceCents": 150},
                            {"product": "Tea", "quantity": 1, "unitPriceCents": 450}]}],
               "nextCursor": {"placedAt": "2026-01-12T18:00:00Z", "id": 4}}'::jsonb)$$
);

SELECT dojo.query_eq(
    'the last page has a null nextCursor',
    'page_json.sql',
    $$VALUES ('{"customer": {"id": 1, "name": "Ada"},
               "orders": [
                 {"id": 2, "placedAt": "2026-01-08T09:30:00Z", "totalCents": 800,
                  "items": [{"product": "Notebook", "quantity": 1, "unitPriceCents": 800}]},
                 {"id": 1, "placedAt": "2026-01-05T10:00:00Z", "totalCents": 2100,
                  "items": [{"product": "Mug", "quantity": 1, "unitPriceCents": 1200},
                            {"product": "Tea", "quantity": 2, "unitPriceCents": 450}]}],
               "nextCursor": null}'::jsonb)$$,
    setup => $$UPDATE page_request SET after_placed_at = '2026-01-12 18:00:00+00', after_id = 4$$
);

SELECT dojo.query_eq(
    'a page that exactly fills the rest has no next cursor, and placedAt stays UTC',
    'page_json.sql',
    $$VALUES ('{"customer": {"id": 2, "name": "Linus"},
               "orders": [{"id": 3, "placedAt": "2026-01-09T12:00:00Z", "totalCents": 450,
                           "items": [{"product": "Pen", "quantity": 3, "unitPriceCents": 150}]}],
               "nextCursor": null}'::jsonb)$$,
    setup => $$SET LOCAL TIME ZONE 'America/Sao_Paulo';
                UPDATE page_request SET customer_id = 2, page_size = 1$$
);

SELECT dojo.query_eq(
    'an empty page is an empty list',
    'page_json.sql',
    $$VALUES ('{"customer": {"id": 1, "name": "Ada"}, "orders": [], "nextCursor": null}'::jsonb)$$,
    setup => $$UPDATE page_request SET after_placed_at = '2026-01-01 00:00:00+00', after_id = 1$$
);

SELECT dojo.script_eq(
    'existing orders become placed and new ones default to placed',
    'migration.sql',
    $$SELECT status, count(*) FROM orders GROUP BY status$$,
    $$VALUES ('placed', 7)$$,
    after => $$INSERT INTO orders (id, customer_id, placed_at) VALUES (7, 2, now())$$
);

SELECT dojo.script_fails(
    'an unknown status is refused',
    'migration.sql',
    $$UPDATE orders SET status = 'lost' WHERE id = 1$$,
    '23514',
    'status must only allow placed, shipped, delivered or cancelled'
);

SELECT dojo.script_ok(
    'the page index matches the screen order',
    'migration.sql',
    $$SELECT EXISTS (SELECT 1 FROM pg_indexes
                     WHERE tablename = 'orders' AND indexname = 'orders_page_idx'
                       AND indexdef LIKE '%(customer_id, placed_at DESC, id DESC)%')
       AND (SELECT is_nullable = 'NO' AND column_default LIKE '''placed''%'
            FROM information_schema.columns
            WHERE table_name = 'orders' AND column_name = 'status')$$,
    'expected orders_page_idx on (customer_id, placed_at DESC, id DESC) and a NOT NULL status defaulting to placed'
);
