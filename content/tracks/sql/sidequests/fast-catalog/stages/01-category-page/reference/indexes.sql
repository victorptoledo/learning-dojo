-- The category page: equality on category_id, then the sort columns, so the index hands the
-- rows over in order and LIMIT stops after 20. Only active products are ever listed.
CREATE INDEX products_category_price_idx ON products (category_id, price, id) WHERE active;

-- Lookups by sku ignore case, and so does uniqueness: one expression index does both.
CREATE UNIQUE INDEX products_sku_lower_key ON products (lower(sku));
