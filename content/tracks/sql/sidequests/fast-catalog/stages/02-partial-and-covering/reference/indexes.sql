-- The category page: equality on category_id, then the sort columns, so the index hands the
-- rows over in order and LIMIT stops after 20. Only active products are ever listed.
CREATE INDEX products_category_price_idx ON products (category_id, price, id) WHERE active;

-- Lookups by sku ignore case, and so does uniqueness: one expression index does both.
CREATE UNIQUE INDEX products_sku_lower_key ON products (lower(sku));

-- Low stock: about 2% of the active products. A partial index holds only those rows, so it
-- is tiny; with sku in INCLUDE the query never touches the table.
CREATE INDEX products_low_stock_idx ON products (stock, id) INCLUDE (sku)
    WHERE active AND stock < 5;

-- The brand summary needs brand and active to find the rows and price to aggregate them:
-- all three in the index make an index-only scan.
CREATE INDEX products_brand_active_idx ON products (brand, active) INCLUDE (price);
