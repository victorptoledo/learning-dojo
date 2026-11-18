-- Products: the SKU, the status and the tags are checked by their types.

CREATE DOMAIN sku AS text
    CHECK (VALUE ~ '^[A-Z]{3}-[0-9]{4}$');

CREATE TYPE product_status AS ENUM ('draft', 'active', 'retired');

CREATE TYPE currency AS ENUM ('EUR', 'USD', 'BRL');

-- At most 5 tags, each made of lowercase letters, digits and dashes, none repeated.
CREATE FUNCTION valid_tags(tags text[]) RETURNS boolean
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT cardinality(tags) <= 5
       AND NOT EXISTS (SELECT FROM unnest(tags) AS t WHERE t IS NULL OR t !~ '^[a-z0-9-]+$')
       AND (SELECT count(DISTINCT t) FROM unnest(tags) AS t) = cardinality(tags)
$$;

CREATE DOMAIN tag_list AS text[]
    CHECK (valid_tags(VALUE));

CREATE TABLE products (
    id     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku    sku NOT NULL UNIQUE,
    name   text NOT NULL CHECK (btrim(name) <> ''),
    status product_status NOT NULL DEFAULT 'draft',
    tags   tag_list NOT NULL DEFAULT '{}'
);

-- Discounts: the rule is a JSONB document whose shape the table checks.

-- A whole JSON number between lo and hi.
CREATE FUNCTION json_whole(v jsonb, lo integer, hi integer) RETURNS boolean
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN jsonb_typeof(v) = 'number'
                THEN v::numeric = trunc(v::numeric) AND v::numeric BETWEEN lo AND hi
                ELSE false END
$$;

CREATE FUNCTION valid_rule(r jsonb) RETURNS boolean
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE
        WHEN jsonb_typeof(r) <> 'object' THEN false
        WHEN r ->> 'kind' = 'percent' THEN
            r - ARRAY['kind', 'value'] = '{}' AND json_whole(r -> 'value', 1, 90)
        WHEN r ->> 'kind' = 'bulk' THEN
            r - ARRAY['kind', 'min_qty', 'value'] = '{}'
            AND json_whole(r -> 'value', 1, 90) AND json_whole(r -> 'min_qty', 2, 2147483647)
        ELSE false
    END
$$;

CREATE TABLE discounts (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id bigint NOT NULL REFERENCES products,
    rule       jsonb NOT NULL CHECK (valid_rule(rule)),
    kind       text GENERATED ALWAYS AS (rule ->> 'kind') STORED
);
