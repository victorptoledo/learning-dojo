-- GIN indexes every element of the array and every key and value of the document, so
-- containment (@>) finds the few matching rows without reading the others.
CREATE INDEX properties_features ON properties USING gin (features);
CREATE INDEX properties_attrs ON properties USING gin (attrs jsonb_path_ops);

-- Trigrams: every three-letter piece of the title, so a substring or a similar word finds
-- the few titles that share its pieces. GIN serves ILIKE '%...%' and the similarity operators.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX properties_title_trgm ON properties USING gin (title gin_trgm_ops);

-- GiST keeps nested boxes of points: it answers "inside this circle" by skipping whole boxes,
-- and "nearest first" (<->) by walking the boxes in order of distance, without a sort.
CREATE INDEX properties_location ON properties USING gist (location);

-- The log is appended in time order, so each block of 128 pages covers a narrow stretch of
-- time. BRIN keeps one summary (min and max observed_at) per block: a few kilobytes for a
-- million rows, against about 22 MB for a B-tree.
CREATE INDEX price_history_observed_at ON price_history USING brin (observed_at);
