-- Page views: one row per product, updated in place (HOT) and never losing a click.

-- A bit more than half of every page stays free, so each row has room for its next version
-- on its own page.
-- No index besides the primary key, so updating views and last_viewed_at stays HOT.
CREATE TABLE product_stats (
    product_id     bigint PRIMARY KEY REFERENCES products,
    views          bigint NOT NULL DEFAULT 0,
    last_viewed_at timestamptz
) WITH (fillfactor = 45);

-- One statement: the insert of the first view and the increment of the next ones cannot race.
-- views + 1 is computed after waiting for the row's lock, on the latest committed version.
CREATE FUNCTION record_view(p_product bigint) RETURNS bigint
LANGUAGE sql AS $$
    INSERT INTO product_stats AS s (product_id, views, last_viewed_at)
    VALUES (p_product, 1, now())
    ON CONFLICT (product_id) DO UPDATE
        SET views = s.views + 1, last_viewed_at = now()
    RETURNING views
$$;
