-- Helpers of the fast-catalog tests (no tests here). Loaded before the stage files.

CREATE SCHEMA catalog_test;

-- The shared buffers (hit + read) a query touches when it runs: the work its plan does.
CREATE FUNCTION catalog_test.buffers(query text) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    plan json;
BEGIN
    EXECUTE 'EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) ' || query INTO plan;
    RETURN (plan -> 0 -> 'Plan' ->> 'Shared Hit Blocks')::bigint
         + (plan -> 0 -> 'Plan' ->> 'Shared Read Blocks')::bigint;
END
$$;

-- True when the query of `file` reads at most `budget` buffers; otherwise fails the test with
-- what it read and its plan, so the learner sees what to fix.
CREATE FUNCTION catalog_test.within(file text, budget integer) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    query text := dojo.sql_of(file);
    used bigint := catalog_test.buffers(query);
BEGIN
    IF used > budget THEN
        RAISE EXCEPTION E'% read % buffers, the budget is %. Its plan:\n%',
            file, used, budget, dojo.plan(query);
    END IF;
    RETURN true;
END
$$;

-- Tests that change data work on a private copy: a temporary table of the same name comes
-- first in the search path. Changing the real table, even in a transaction rolled back,
-- clears its visibility map and would slow the index-only scans of the later tests.
CREATE FUNCTION catalog_test.private_copy(tbl text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE format('CREATE TEMP TABLE %I AS SELECT * FROM public.%I', tbl, tbl);
END
$$;
