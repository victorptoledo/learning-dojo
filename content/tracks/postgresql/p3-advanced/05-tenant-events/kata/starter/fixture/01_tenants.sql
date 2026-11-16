-- The tenants of a SaaS product. Your schema.sql creates their events table.
-- The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE tenants (
    id   integer PRIMARY KEY,
    name text NOT NULL
);

INSERT INTO tenants (id, name) VALUES (1, 'Acme'), (2, 'Globex'), (3, 'Initech');
