-- Stage 3 contract: transfers.sql adds transfer(), which refuses invalid transfers and never
-- takes a customer below zero, also when two sessions spend the same money at once.

-- A transfer that reports how it ended: 'ok', or the SQLSTATE it failed with. Sessions call it
-- so the tests can tell LD001 from any other error.
CREATE FUNCTION ledger_test.try_transfer(p_from bigint, p_to bigint, p_amount numeric) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM transfer(p_from, p_to, p_amount, 'gift');
    RETURN 'ok';
EXCEPTION WHEN OTHERS THEN
    RETURN SQLSTATE || ': ' || SQLERRM;
END
$$;

-- Alice (2) has 100.00. Session A moves 70 from alice to bob in an open transaction; session
-- B moves 70 from `b_from` to `b_to`; A then ends with first_ends.
CREATE FUNCTION ledger_test.two_payments(b_from integer, b_to integer, first_ends text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    alice numeric;
BEGIN
    PERFORM ledger_test.reset();
    PERFORM ledger_test.open('a', 'b');
    PERFORM ledger_test.run('a', 'SELECT transfer(1, 2, 100, ''deposit'')');
    PERFORM ledger_test.run('a', 'SELECT transfer(1, 4, 100, ''deposit'')');
    PERFORM dblink_exec('a', 'BEGIN');
    a := ledger_test.run('a', 'SELECT transfer(2, 3, 70, ''rent'')');
    PERFORM ledger_test.expect(a IS NOT NULL AND a NOT LIKE 'error:%',
        format('session A''s transfer of 70 from alice should go through, got %s', coalesce(a, 'NULL')));
    PERFORM ledger_test.start('b', format('SELECT ledger_test.try_transfer(%s, %s, 70)', b_from, b_to));
    b_waited := ledger_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := ledger_test.finish('b');
    PERFORM ledger_test.close_all();
    alice := balance(2);
    IF b_from <> 2 THEN
        PERFORM ledger_test.expect(NOT b_waited,
            'a transfer from carol to the bank waited for a transfer from alice to bob: lock only the accounts a transfer touches');
        PERFORM ledger_test.expect(b = 'ok', 'carol''s transfer should go through: ' || coalesce(b, 'NULL'));
    ELSE
        PERFORM ledger_test.expect(b_waited,
            'session B should wait for session A''s open transfer from the same account, but it finished at once with ' || coalesce(b, 'NULL'));
        IF first_ends = 'COMMIT' THEN
            PERFORM ledger_test.expect(b LIKE 'LD001:%',
                format('after A spent 70 of alice''s 100, B''s 70 should fail with LD001, got %s', coalesce(b, 'NULL')));
            PERFORM ledger_test.expect(alice = 30, format('alice should end with 30.00, she has %s', alice));
        ELSE
            PERFORM ledger_test.expect(b = 'ok',
                format('A rolled back, so B''s transfer should go through, got %s', coalesce(b, 'NULL')));
            PERFORM ledger_test.expect(alice = 30, format('alice should end with 30.00, she has %s', alice));
        END IF;
    END IF;
    PERFORM ledger_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM ledger_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_eq(
    'a transfer moves money',
    NULL,
    $$SELECT j.memo, p.account_id, p.amount, balance(p.account_id)
      FROM journal j JOIN postings p ON p.journal_id = j.id
      WHERE j.id = (SELECT id FROM r) ORDER BY p.account_id$$,
    $$VALUES ('rent', 2::bigint, -40.00, 60.00), ('rent', 3, 40.00, 40.00)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$,
    after => $$CREATE TEMP TABLE r AS SELECT transfer(2, 3, 40, 'rent') AS id;
               SET CONSTRAINTS ALL IMMEDIATE$$
);

SELECT dojo.script_eq(
    'the bank may go below zero',
    NULL,
    $$SELECT balance(1), balance(2)$$,
    $$VALUES (-100.00, 100.00)$$,
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql')$$,
    after => $$SELECT transfer(1, 2, 100, 'deposit')$$
);

SELECT dojo.script_ok(
    'a customer may spend everything',
    NULL,
    $$SELECT balance(2) = 0$$,
    'alice should be able to transfer all of her 100.00',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$,
    after => $$SELECT transfer(2, 3, 100, 'everything')$$
);

SELECT dojo.script_fails('a customer cannot go below zero', NULL,
    $$SELECT transfer(2, 3, 100.01, 'too much')$$, 'LD001',
    'alice has 100.00: a transfer of 100.01 must fail with LD001',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$);

SELECT dojo.script_fails('an empty account cannot pay', NULL,
    $$SELECT transfer(4, 3, 1, 'nothing')$$, 'LD001',
    'carol has nothing: any transfer from her must fail with LD001',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql')$$);

SELECT dojo.script_ok(
    'a refused transfer posts nothing',
    NULL,
    $$SELECT count(*) = 1 FROM journal$$,
    'after the refused transfer, only the deposit should be in the journal',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$,
    after => $$DO $do$ BEGIN
                   PERFORM transfer(2, 3, 500, 'too much');
               EXCEPTION WHEN SQLSTATE 'LD001' THEN NULL;
               END $do$ $$
);

SELECT dojo.script_fails('the amount must be positive', NULL,
    $$SELECT transfer(2, 3, 0, 'nothing')$$, '22023',
    'a transfer of 0 is invalid',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$);

SELECT dojo.script_fails('a negative amount is not a way to take money', NULL,
    $$SELECT transfer(2, 3, -50, 'reverse')$$, '22023',
    'a transfer of -50 from alice to bob would take bob''s money',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 3, 100, 'deposit')$$);

SELECT dojo.script_fails('an account cannot pay itself', NULL,
    $$SELECT transfer(2, 2, 10, 'loop')$$, '22023',
    'from and to are the same account',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$);

SELECT dojo.script_fails('currencies must match', NULL,
    $$SELECT transfer(2, 6, 10, 'fx')$$, '22023',
    'alice''s account 2 is in EUR and dave''s 6 in USD',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$);

SELECT dojo.script_fails('accounts must exist', NULL,
    $$SELECT transfer(2, 99, 10, 'nowhere')$$, '22023',
    'account 99 does not exist',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql');
               SELECT transfer(1, 2, 100, 'deposit')$$);

SELECT dojo.script_ok('two payments cannot spend the same money', NULL,
    $$SELECT ledger_test.two_payments(2, 4, 'COMMIT')$$, 'see above',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql')$$);

SELECT dojo.script_ok('a rolled back payment leaves the money to the next', NULL,
    $$SELECT ledger_test.two_payments(2, 4, 'ROLLBACK')$$, 'see above',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql')$$);

SELECT dojo.script_ok('payments of other accounts do not wait', NULL,
    $$SELECT ledger_test.two_payments(4, 1, 'COMMIT')$$, 'see above',
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql')$$);
