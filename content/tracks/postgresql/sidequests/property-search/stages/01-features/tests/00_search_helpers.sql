-- Helpers of the property-search tests (no tests here). Loaded before the stage files.
--
-- indexes.sql is loaded once and committed here, then the tables are vacuumed and analyzed,
-- as autovacuum would have done long ago in production. A failed load is remembered and
-- reported by every test that needs the indexes. The tests run inside the usual rolled-back
-- block and only read.

CREATE SCHEMA search_test;

CREATE TABLE search_test.load_errors (
    file    text PRIMARY KEY,
    message text NOT NULL
);

CREATE FUNCTION search_test.load(file text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE dojo.sql_of(file);
    EXCEPTION WHEN OTHERS THEN
        INSERT INTO search_test.load_errors
        VALUES (file, CASE WHEN SQLERRM LIKE 'todo:%' OR SQLERRM LIKE 'the file % is missing%'
                           THEN SQLERRM
                           ELSE format('%s does not load: %s (SQLSTATE %s)', file, SQLERRM, SQLSTATE) END);
    END;
END
$$;

-- Raises the load error of a file, if it had one: the first step of every test.
CREATE FUNCTION search_test.require(VARIADIC files text[]) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    problem text;
BEGIN
    SELECT message INTO problem FROM search_test.load_errors WHERE file = ANY (files) LIMIT 1;
    IF problem IS NOT NULL THEN
        RAISE EXCEPTION '%', problem;
    END IF;
END
$$;

-- The shared buffers (hit + read) a query touches when it runs: the work its plan does.
CREATE FUNCTION search_test.buffers(query text) RETURNS bigint
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
CREATE FUNCTION search_test.within(file text, budget integer) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    query text := dojo.sql_of(file);
    used bigint := search_test.buffers(query);
BEGIN
    IF used > budget THEN
        RAISE EXCEPTION E'% read % buffers, the budget is %. Its plan:\n%',
            file, used, budget, dojo.plan(query);
    END IF;
    RETURN true;
END
$$;

SELECT search_test.load('indexes.sql');

-- A separate statement, outside any transaction: the visibility map is set and the planner
-- has statistics, indexes included.
VACUUM ANALYZE;
