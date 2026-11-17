-- The same column as the primary key: the key's own index already does this job.
DROP INDEX products_id_idx;
-- The leading column of products_category_price_idx, which serves every category query.
DROP INDEX products_category_idx;
-- Two values, nine rows in ten true: a scan of the table is cheaper than this index.
DROP INDEX products_active_idx;
-- Five values, each a fifth of the reviews: no query of the shop is helped by it.
DROP INDEX reviews_rating_idx;
