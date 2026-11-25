-- A GIN index answers containment (@>) on the whole document; jsonb_path_ops is smaller and
-- serves @> and @? only, which is all the color search needs.
CREATE INDEX products_attrs ON products USING gin (attrs jsonb_path_ops);

-- An expression index on the brand, followed by the ORDER BY columns, hands the rows of one
-- brand back already sorted by (price, id): the LIMIT stops after 10 index entries, no Sort.
CREATE INDEX products_brand_price ON products ((attrs ->> 'brand'), price, id);
