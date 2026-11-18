-- A property portal: 100,000 listings and 1,000,000 price observations, generated the same way
-- on every check (setseed). Only the primary keys exist; the indexes are yours.

-- The dojo's server commits asynchronously; this session waits for each commit, so the vacuum
-- the tests run after loading indexes.sql can mark every page all-visible right away.
SET synchronous_commit = on;

SELECT setseed(0.42);

CREATE TABLE properties (
    id        bigint PRIMARY KEY,
    title     text NOT NULL,
    city      text NOT NULL,
    kind      text NOT NULL,
    bedrooms  integer NOT NULL,
    price     integer NOT NULL,
    features  text[] NOT NULL,
    attrs     jsonb NOT NULL,
    location  point NOT NULL,
    listed_at timestamptz NOT NULL
);

-- Place names are three syllables: 8,000 of them, about 12 listings each.
CREATE FUNCTION fixture_place(n integer) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
    SELECT initcap(s[1 + n % 20] || s[1 + (n / 20) % 20] || s[1 + (n / 400) % 20])
    FROM (SELECT ARRAY['ma', 're', 'lo', 'vi', 'sa', 'no', 'ta', 'ri', 'ca', 'be',
                       'du', 'fi', 'go', 'la', 'me', 'pi', 'ru', 'so', 'te', 'zu'] AS s) AS syllables
$$;

INSERT INTO properties
SELECT i,
       adjective || ' ' || kind || ' in ' || fixture_place((i * 7919) % 8000),
       city, kind, bedrooms,
       (CASE kind WHEN 'studio' THEN 90000 WHEN 'apartment' THEN 180000 WHEN 'loft' THEN 240000 ELSE 320000 END
        + bedrooms * 25000 + (random() * 150000)::integer) / 100 * 100,
       features, attrs,
       point(round((random() * 100)::numeric, 4), round((random() * 100)::numeric, 4)),
       timestamptz '2020-01-01' + random() * interval '2000 days'
FROM (
    SELECT i,
           (ARRAY['Bright', 'Cozy', 'Spacious', 'Quiet', 'Modern', 'Charming', 'Sunny', 'Renovated',
                  'Elegant', 'Rustic', 'Airy', 'Classic', 'Stylish', 'Compact', 'Grand', 'Peaceful'])[1 + (random() * 15)::integer] AS adjective,
           (ARRAY['Lisbon', 'Porto', 'Braga', 'Coimbra', 'Faro', 'Aveiro', 'Evora', 'Setubal', 'Leiria', 'Viseu',
                  'Guimaraes', 'Funchal', 'Sintra', 'Cascais', 'Tavira', 'Lagos', 'Nazare', 'Obidos', 'Tomar', 'Viana'])[1 + (random() * 19)::integer] AS city,
           (ARRAY['apartment', 'apartment', 'apartment', 'house', 'house', 'studio', 'loft'])[1 + (random() * 6)::integer] AS kind,
           1 + (random() * 4)::integer AS bedrooms,
           ARRAY(SELECT f FROM (VALUES ('garage', 0.30), ('balcony', 0.40), ('garden', 0.20), ('pool', 0.03),
                                       ('elevator', 0.35), ('terrace', 0.15), ('fireplace', 0.08), ('solar-panels', 0.05),
                                       ('sea-view', 0.02), ('gym', 0.04), ('storage', 0.25), ('air-conditioning', 0.30))
                                AS f(f, p)
                 WHERE random() < p + i * 0) AS features,
           jsonb_build_object('floor', (random() * 20)::integer,
                              'pets', random() < 0.5,
                              'energy', (ARRAY['A', 'B', 'B', 'C', 'C', 'C', 'D', 'D', 'E', 'F', 'G'])[1 + (random() * 10)::integer],
                              'furnished', random() < 0.3) AS attrs
    FROM generate_series(1, 100000) AS i
) AS p;

DROP FUNCTION fixture_place(integer);

-- Every 5 minutes, someone's asking price is observed, from 2016 to the end of 2025: the rows
-- arrive in time order, as an append-only log does.
CREATE TABLE price_history (
    property_id bigint NOT NULL REFERENCES properties,
    observed_at timestamptz NOT NULL,
    price       integer NOT NULL
);

INSERT INTO price_history
SELECT 1 + (random() * 99999)::integer,
       timestamptz '2016-01-01' + i * interval '5 minutes',
       (100000 + random() * 500000)::integer / 100 * 100
FROM generate_series(0, 999999) AS i;
