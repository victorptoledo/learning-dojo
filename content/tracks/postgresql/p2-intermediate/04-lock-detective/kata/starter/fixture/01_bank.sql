-- A small bank. The tests load this file into a fresh PostgreSQL 17, then open other sessions
-- (with the dblink extension) that hold and wait for locks while your queries run.
CREATE TABLE accounts (
    id      bigint PRIMARY KEY,
    owner   text NOT NULL,
    balance numeric(12, 2) NOT NULL
);

INSERT INTO accounts (id, owner, balance)
SELECT i, format('owner %s', i), (i * 37) % 1000
FROM generate_series(1, 1000) AS i;
