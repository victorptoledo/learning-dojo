-- The bank's accounts. Customer accounts belong to a person; system accounts (the bank's own
-- cash, one per currency) are where money enters and leaves, and may go below zero.
CREATE TABLE accounts (
    id       bigint PRIMARY KEY,
    owner    text NOT NULL,
    currency text NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    kind     text NOT NULL CHECK (kind IN ('customer', 'system'))
);

INSERT INTO accounts (id, owner, currency, kind) VALUES
    (1, 'bank', 'EUR', 'system'),
    (2, 'alice', 'EUR', 'customer'),
    (3, 'bob', 'EUR', 'customer'),
    (4, 'carol', 'EUR', 'customer'),
    (5, 'bank', 'USD', 'system'),
    (6, 'dave', 'USD', 'customer'),
    (7, 'alice', 'USD', 'customer');
