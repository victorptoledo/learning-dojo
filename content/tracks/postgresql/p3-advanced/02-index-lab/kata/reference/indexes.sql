-- Time ranges over an append-only table: BRIN stores one min/max per block range.
CREATE INDEX events_at_brin ON events USING brin (at);

-- Containment in arrays and documents: GIN.
CREATE INDEX products_tags ON products USING gin (tags);
CREATE INDEX products_attrs ON products USING gin (attrs jsonb_path_ops);

-- One account per email, whatever the capitalisation.
CREATE UNIQUE INDEX users_email_lower ON users (lower(email));

-- Only the 0.1% of jobs the queue reads, already in the order it wants.
CREATE INDEX jobs_pending ON jobs (run_at) WHERE status = 'pending';

-- Fragments anywhere in the name: trigrams.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX customers_name_trgm ON customers USING gin (name gin_trgm_ops);

-- The account page reads only customer_id and total: an index-only scan.
CREATE INDEX orders_customer ON orders (customer_id) INCLUDE (total);
