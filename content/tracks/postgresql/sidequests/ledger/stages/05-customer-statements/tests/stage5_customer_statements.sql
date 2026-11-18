-- Stage 5 contract: customers.sql creates the read-only ledger_customer role, policies that
-- show a customer only their own accounts, postings and entries, and statement().

-- The test helpers run as the customer role once a test switched to it: let it reach them.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'ledger_customer') THEN
        GRANT USAGE ON SCHEMA dojo TO ledger_customer;
    END IF;
END
$$;

-- Two months of history, written by hand (the tests' transaction is rolled back anyway).
-- alice: 2 (EUR) and 7 (USD); bob: 3; carol: 4.
CREATE FUNCTION ledger_test.history() RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    entry record;
    j bigint;
BEGIN
    FOR entry IN
        SELECT * FROM (VALUES
            ('2026-01-05 09:00'::timestamptz, 'deposit', 1, 2, 100.00),
            ('2026-01-10 12:00', 'rent', 2, 3, 40.00),
            ('2026-02-01 08:00', 'salary', 1, 3, 500.00),
            ('2026-02-03 13:00', 'lunch', 3, 2, 20.00),
            ('2026-02-15 10:00', 'usd deposit', 5, 7, 50.00),
            ('2026-02-28 23:30', 'coffee', 2, 4, 5.00),
            ('2026-03-01 00:00', 'march', 2, 3, 10.00)) AS e(at, memo, from_account, to_account, amount)
    LOOP
        INSERT INTO journal (memo, posted_at) VALUES (entry.memo, entry.at) RETURNING id INTO j;
        INSERT INTO postings (journal_id, account_id, amount)
        VALUES (j, entry.from_account, -entry.amount), (j, entry.to_account, entry.amount);
    END LOOP;
END
$$;

SELECT dojo.script_eq(
    'a customer sees their own accounts',
    NULL,
    $$SELECT id, owner FROM accounts ORDER BY id$$,
    $$VALUES (2::bigint, 'alice'), (7, 'alice')$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql')$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$
);

SELECT dojo.script_eq(
    'a customer sees the postings of their accounts',
    NULL,
    $$SELECT account_id, count(*) FROM postings GROUP BY account_id ORDER BY account_id$$,
    $$VALUES (3::bigint, 4::bigint)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'bob'$$
);

SELECT dojo.script_eq(
    'a customer sees the entries that touch their accounts',
    NULL,
    $$SELECT memo FROM journal ORDER BY posted_at$$,
    $$VALUES ('deposit'), ('rent'), ('lunch'), ('usd deposit'), ('coffee'), ('march')$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$
);

SELECT dojo.script_eq(
    'no customer means no rows',
    NULL,
    $$SELECT (SELECT count(*) FROM accounts), (SELECT count(*) FROM postings), (SELECT count(*) FROM journal)$$,
    $$VALUES (0::bigint, 0::bigint, 0::bigint)$$,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer$$
);

SELECT dojo.script_eq(
    'an empty customer means no rows',
    NULL,
    $$SELECT (SELECT count(*) FROM accounts), (SELECT count(*) FROM postings), (SELECT count(*) FROM journal)$$,
    $$VALUES (0::bigint, 0::bigint, 0::bigint)$$,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = ''$$
);

SELECT dojo.script_eq(
    'balance shows only what the customer may see',
    NULL,
    $$SELECT balance(2), balance(7), balance(3)$$,
    $$VALUES (65.00, 50.00, 0)$$,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$
);

SELECT dojo.script_eq(
    'a statement has a running balance from the opening one',
    NULL,
    $$SELECT * FROM statement(2, '2026-02-01', '2026-02-28')$$,
    $$VALUES ('2026-02-03 13:00'::timestamptz, 'lunch', 20.00, 80.00),
             ('2026-02-28 23:30', 'coffee', -5.00, 75.00)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$
);

SELECT dojo.script_eq(
    'a statement includes its first and last days',
    NULL,
    $$SELECT * FROM statement(2, '2026-01-05', '2026-01-10')$$,
    $$VALUES ('2026-01-05 09:00'::timestamptz, 'deposit', 100.00, 100.00),
             ('2026-01-10 12:00', 'rent', -40.00, 60.00)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$
);

SELECT dojo.script_eq(
    'a statement of someone else''s account is empty',
    NULL,
    $$SELECT count(*) FROM statement(3, '2026-01-01', '2026-12-31')$$,
    $$VALUES (0::bigint)$$,
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql');
               SELECT ledger_test.history()$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$
);

SELECT dojo.script_fails('a customer cannot post', NULL,
    $$INSERT INTO journal (memo) VALUES ('free money')$$, '42501',
    'ledger_customer may only read',
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql')$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$);

SELECT dojo.script_fails('a customer cannot change an account', NULL,
    $$UPDATE accounts SET owner = 'alice' WHERE id = 3$$, '42501',
    'ledger_customer may only read',
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql')$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$);

SELECT dojo.script_fails('a customer cannot delete postings', NULL,
    $$DELETE FROM postings$$, '42501',
    'ledger_customer may only read',
    setup => $$SELECT ledger_test.require('ledger.sql', 'customers.sql')$$,
    after => $$SET LOCAL ROLE ledger_customer;
               SET LOCAL app.customer = 'alice'$$);
