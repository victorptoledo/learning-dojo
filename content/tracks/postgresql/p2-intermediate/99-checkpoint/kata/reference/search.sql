-- The searchable text, computed on every write and indexed. The configuration is named, so
-- the expression is immutable.
ALTER TABLE tickets ADD COLUMN doc tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('english', title), 'A') ||
    setweight(to_tsvector('english', body), 'B')
) STORED;

CREATE INDEX tickets_doc ON tickets USING gin (doc);

-- Containment filters on the metadata (metadata @> '{...}').
CREATE INDEX tickets_metadata ON tickets USING gin (metadata jsonb_path_ops);
