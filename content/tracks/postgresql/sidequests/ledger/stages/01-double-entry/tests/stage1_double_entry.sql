-- Stage 1 contract: ledger.sql creates journal and postings, whose entries must balance at
-- COMMIT, post(), which refuses an unbalanced entry at once, and balance().

-- In a real session: a journal entry and the given postings, then COMMIT. Returns the
-- SQLSTATE the commit failed with ('00000' when it committed), and the entries that stayed.
CREATE FUNCTION ledger_test.commit_entry(postings text) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    state text := '00000';
    kept bigint;
BEGIN
    PERFORM ledger_test.reset();
    PERFORM ledger_test.open('a');
    PERFORM dblink_exec('a', 'BEGIN');
    PERFORM dblink_exec('a', 'INSERT INTO journal (memo) VALUES (''by hand'')');
    IF postings <> '' THEN
        PERFORM dblink_exec('a', format(
            'INSERT INTO postings (journal_id, account_id, amount) SELECT max(j.id), p.a, p.m FROM journal j, (VALUES %s) AS p(a, m) GROUP BY p.a, p.m',
            postings));
    END IF;
    BEGIN
        PERFORM dblink_exec('a', 'COMMIT');
    EXCEPTION WHEN OTHERS THEN
        state := SQLSTATE;
    END;
    PERFORM ledger_test.close_all();
    SELECT count(*) INTO kept FROM journal;
    PERFORM ledger_test.reset();
    RETURN format('%s, %s kept', state, kept);
EXCEPTION WHEN OTHERS THEN
    PERFORM ledger_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_eq(
    'post writes a balanced entry',
    NULL,
    $$SELECT j.memo, j.posted_at = now(), p.account_id, p.amount
      FROM journal j JOIN postings p ON p.journal_id = j.id
      WHERE j.id = (SELECT id FROM r) ORDER BY p.account_id$$,
    $$VALUES ('lunch', true, 2::bigint, -25.00), ('lunch', true, 3, 25.00)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql')$$,
    after => $$CREATE TEMP TABLE r AS
               SELECT post('lunch', '[{"account": 2, "amount": -25.00}, {"account": 3, "amount": 25.00}]') AS id$$
);

SELECT dojo.script_ok(
    'an entry may have more than two postings',
    NULL,
    $$SELECT count(*) = 3 FROM postings$$,
    'post with three postings that add up to zero should be accepted',
    setup => $$SELECT ledger_test.require('ledger.sql')$$,
    after => $$SELECT post('dinner', '[{"account": 2, "amount": -60}, {"account": 3, "amount": 30}, {"account": 4, "amount": 30}]')$$
);

SELECT dojo.script_fails('post refuses an unbalanced entry', NULL,
    $$SELECT post('oops', '[{"account": 2, "amount": -25.00}, {"account": 3, "amount": 20.00}]')$$, '23514',
    'the postings add up to -5.00: post must refuse the entry right away',
    setup => $$SELECT ledger_test.require('ledger.sql')$$);

SELECT dojo.script_fails('post refuses a single posting', NULL,
    $$SELECT post('alone', '[{"account": 2, "amount": 10}]')$$, '23514',
    'an entry needs at least two postings',
    setup => $$SELECT ledger_test.require('ledger.sql')$$);

SELECT dojo.script_fails('post refuses an entry with no postings', NULL,
    $$SELECT post('empty', '[]')$$, '23514',
    'an entry needs at least two postings',
    setup => $$SELECT ledger_test.require('ledger.sql')$$);

SELECT dojo.script_fails('post refuses two currencies', NULL,
    $$SELECT post('fx', '[{"account": 2, "amount": -10}, {"account": 6, "amount": 10}]')$$, '23514',
    'account 2 is in EUR and account 6 in USD: one entry, one currency',
    setup => $$SELECT ledger_test.require('ledger.sql')$$);

SELECT dojo.script_fails('a posting of zero is refused', NULL,
    $$INSERT INTO postings (journal_id, account_id, amount)
      VALUES ((SELECT id FROM journal), 2, 0)$$, '23514',
    'a posting of 0.00 moves nothing',
    setup => $$SELECT ledger_test.require('ledger.sql');
               INSERT INTO journal (memo) VALUES ('zero')$$);

SELECT dojo.script_fails('a posting needs an account', NULL,
    $$INSERT INTO postings (journal_id, account_id, amount)
      VALUES ((SELECT id FROM journal), 99, 5)$$, '23503',
    'account 99 does not exist',
    setup => $$SELECT ledger_test.require('ledger.sql');
               INSERT INTO journal (memo) VALUES ('nowhere')$$);

SELECT dojo.script_ok(
    'an entry may be written row by row',
    NULL,
    $$SELECT count(*) = 2 FROM postings$$,
    'a journal row, then one posting, then the other, then SET CONSTRAINTS ALL IMMEDIATE should all be accepted: the rule waits for the end of the transaction',
    setup => $$SELECT ledger_test.require('ledger.sql')$$,
    after => $$INSERT INTO journal (memo) VALUES ('by hand');
               INSERT INTO postings (journal_id, account_id, amount) VALUES ((SELECT id FROM journal), 2, -7.50);
               INSERT INTO postings (journal_id, account_id, amount) VALUES ((SELECT id FROM journal), 4, 7.50);
               SET CONSTRAINTS ALL IMMEDIATE$$
);

SELECT dojo.script_fails('an unbalanced entry fails at commit time', NULL,
    $$SET CONSTRAINTS ALL IMMEDIATE$$, '23514',
    'the entry''s postings add up to -7.50: the check at COMMIT must fail',
    setup => $$SELECT ledger_test.require('ledger.sql');
               INSERT INTO journal (memo) VALUES ('by hand');
               INSERT INTO postings (journal_id, account_id, amount) VALUES ((SELECT id FROM journal), 2, -7.50)$$);

SELECT dojo.script_fails('an entry without postings fails at commit time', NULL,
    $$SET CONSTRAINTS ALL IMMEDIATE$$, '23514',
    'a journal entry with no postings at all must fail at COMMIT',
    setup => $$SELECT ledger_test.require('ledger.sql');
               INSERT INTO journal (memo) VALUES ('nothing')$$);

SELECT dojo.script_eq(
    'balance sums the postings of an account',
    NULL,
    $$SELECT a, balance(a) FROM generate_series(1, 7) AS a ORDER BY a$$,
    $$VALUES (1, -100.00), (2, 75.00), (3, 25.00), (4, 0), (5, -40.00), (6, 40.00), (7, 0)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql')$$,
    after => $$SELECT post('deposit', '[{"account": 1, "amount": -100}, {"account": 2, "amount": 100}]');
               SELECT post('lunch', '[{"account": 2, "amount": -25.00}, {"account": 3, "amount": 25.00}]');
               SELECT post('deposit', '[{"account": 5, "amount": -40}, {"account": 6, "amount": 40}]')$$
);

SELECT dojo.script_eq('a real COMMIT keeps a balanced entry', NULL,
    $$SELECT ledger_test.commit_entry('(2, -5.00), (3, 5.00)')$$,
    $$VALUES ('00000, 1 kept')$$,
    setup => $$SELECT ledger_test.require('ledger.sql')$$);

SELECT dojo.script_eq('a real COMMIT refuses an unbalanced entry', NULL,
    $$SELECT ledger_test.commit_entry('(2, -5.00), (3, 4.00)')$$,
    $$VALUES ('23514, 0 kept')$$,
    setup => $$SELECT ledger_test.require('ledger.sql')$$);

SELECT dojo.script_eq('a real COMMIT refuses an entry without postings', NULL,
    $$SELECT ledger_test.commit_entry('')$$,
    $$VALUES ('23514, 0 kept')$$,
    setup => $$SELECT ledger_test.require('ledger.sql')$$);
