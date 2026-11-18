-- Stage 4 contract: interest.sql creates interest_runs and the apply_interest procedure, which
-- commits in batches and, run again after a failure, never pays an account twice.

-- Fails the posting of one account, to break a run halfway.
CREATE FUNCTION ledger_test.boom() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'the test breaks the run at account %', NEW.account_id;
END
$$;

-- Removes the breaking trigger, from a session of its own. It needs the table to itself, so
-- it must run before this session reads postings; it gives up after 5 s rather than hang.
CREATE FUNCTION ledger_test.drop_boom() RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_connect('cleanup', 'host=/tmp dbname=dojo user=dojo');
    PERFORM dblink_exec('cleanup', 'SET lock_timeout = ''5s''');
    PERFORM dblink_exec('cleanup', 'DROP TRIGGER IF EXISTS ledger_test_boom ON postings');
    PERFORM dblink_disconnect('cleanup');
END
$$;

-- What the ledger holds after a step of a scenario (PL/pgSQL, so that this file loads before
-- interest_runs exists).
CREATE FUNCTION ledger_test.interest_state(p_step text) RETURNS TABLE (
    step text, runs bigint, paid bigint, entries bigint, transactions bigint, bank numeric, lowest numeric, highest numeric)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT p_step,
           (SELECT count(*) FROM interest_runs WHERE month = '2026-01-01'),
           (SELECT count(journal_id) FROM interest_runs WHERE month = '2026-01-01'),
           (SELECT count(*) FROM journal WHERE memo = 'interest 2026-01'),
           (SELECT count(DISTINCT xmin::text) FROM interest_runs),
           balance(1),
           (SELECT min(balance(id)) FROM accounts WHERE id >= 1000),
           (SELECT max(balance(id)) FROM accounts WHERE id >= 1000);
END
$$;

-- 250 new customer accounts (ids 1000 to 1249) with 100.00 each, besides the fixture's five
-- customers with nothing; then the January run at 1%, in batches of 100, twice. With
-- break_at, the first run fails at that account (the 180th customer in id order).
CREATE FUNCTION ledger_test.interest_scenario(break_at bigint) RETURNS SETOF record
LANGUAGE plpgsql AS $$
DECLARE
    first text;
    second text;
BEGIN
    PERFORM ledger_test.reset();
    PERFORM ledger_test.open('a');
    PERFORM dblink_exec('a', 'INSERT INTO accounts (id, owner, currency, kind)
                              SELECT 999 + i, ''customer-'' || i, ''EUR'', ''customer'' FROM generate_series(1, 250) AS i');
    PERFORM ledger_test.run('a', 'SELECT count(transfer(1, id, 100, ''opening'')) FROM accounts WHERE id >= 1000');
    IF break_at IS NOT NULL THEN
        PERFORM dblink_exec('a', format(
            'CREATE TRIGGER ledger_test_boom BEFORE INSERT ON postings FOR EACH ROW WHEN (NEW.account_id = %s) EXECUTE FUNCTION ledger_test.boom()',
            break_at));
    END IF;
    first := ledger_test.run('a', 'CALL apply_interest(''2026-01-01'', 0.01, 100)');
    PERFORM ledger_test.expect((first LIKE 'error:%') = (break_at IS NOT NULL), CASE
        WHEN break_at IS NULL THEN 'CALL apply_interest(''2026-01-01'', 0.01, 100) failed: ' || first
        ELSE 'the run should fail at account ' || break_at || ', where the test breaks it, but it did not' END);
    PERFORM ledger_test.drop_boom();
    RETURN QUERY SELECT * FROM ledger_test.interest_state('first run');
    second := ledger_test.run('a', 'CALL apply_interest(''2026-01-01'', 0.01, 100)');
    PERFORM ledger_test.expect(second IS NULL OR second NOT LIKE 'error:%',
        'running apply_interest again for January failed: ' || coalesce(second, ''));
    RETURN QUERY SELECT * FROM ledger_test.interest_state('second run');
    PERFORM ledger_test.close_all();
    PERFORM ledger_test.reset();
EXCEPTION WHEN OTHERS THEN
    PERFORM ledger_test.close_all();
    PERFORM ledger_test.drop_boom();
    RAISE;
END
$$;

SELECT dojo.script_eq(
    'a month-end run pays every account once',
    NULL,
    $$SELECT * FROM ledger_test.interest_scenario(NULL) AS s(
          step text, runs bigint, paid bigint, entries bigint, transactions bigint, bank numeric, lowest numeric, highest numeric)$$,
    $$VALUES ('first run', 255::bigint, 250::bigint, 250::bigint, 3::bigint, -25250.00, 101.00, 101.00),
             ('second run', 255, 250, 250, 3, -25250.00, 101.00, 101.00)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql', 'interest.sql')$$
);

SELECT dojo.script_eq(
    'a run that fails keeps its batches and the rerun finishes the rest',
    NULL,
    $$SELECT * FROM ledger_test.interest_scenario(1174) AS s(
          step text, runs bigint, paid bigint, entries bigint, transactions bigint, bank numeric, lowest numeric, highest numeric)$$,
    $$VALUES ('first run', 100::bigint, 95::bigint, 95::bigint, 1::bigint, -25095.00, 100.00, 101.00),
             ('second run', 255, 250, 250, 3, -25250.00, 101.00, 101.00)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'transfers.sql', 'interest.sql')$$
);

SELECT dojo.script_ok(
    'interest_runs has one row per month and account',
    NULL,
    $$SELECT EXISTS (
        SELECT FROM pg_index i
        WHERE i.indrelid = 'interest_runs'::regclass AND i.indisunique
          AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
               FROM pg_attribute a WHERE a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)) = '{account_id,month}')$$,
    'interest_runs should have a primary key or unique constraint on (month, account_id)',
    setup => $$SELECT ledger_test.require('interest.sql')$$
);

SELECT dojo.script_ok(
    'apply_interest is a procedure',
    NULL,
    $$SELECT EXISTS (SELECT FROM pg_proc WHERE proname = 'apply_interest' AND prokind = 'p'
                     AND pronamespace = 'public'::regnamespace)$$,
    'apply_interest should be a PROCEDURE: only a procedure can COMMIT between batches',
    setup => $$SELECT ledger_test.require('interest.sql')$$
);
