-- Give up after 2 s instead of queueing every other query behind this migration.
SET LOCAL lock_timeout = '2s';

-- A constant default needs no table rewrite since PostgreSQL 11.
ALTER TABLE accounts ADD COLUMN currency char(3) NOT NULL DEFAULT 'EUR';

-- Check new rows at once, existing rows with a weaker lock.
ALTER TABLE accounts ADD CONSTRAINT accounts_balance_positive CHECK (balance >= 0) NOT VALID;
ALTER TABLE accounts VALIDATE CONSTRAINT accounts_balance_positive;
