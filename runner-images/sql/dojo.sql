-- The dojo's SQL test helpers, loaded by check.sh after the kata's fixture.
--
-- A test file is a list of calls, one per test:
--   SELECT dojo.query_eq('name', 'file.sql', $$VALUES (...)$$, ordered => true);
--   SELECT dojo.script_eq('name', 'file.sql', 'query after the script', $$VALUES (...)$$);
--   SELECT dojo.script_fails('name', 'file.sql', 'statement', '23505', 'why it must fail');
--   SELECT dojo.script_ok('name', 'file.sql', 'boolean query', 'what is wrong when false');
-- Helpers for those strings: dojo.run_file('schema.sql') runs a file of the kata, and
-- dojo.plan('query') returns its EXPLAIN as text.
-- `setup` runs before the learner's file and `after` runs after it. Everything a test does is
-- rolled back (the block ends by raising DJ000), so tests never see each other's changes.
-- Rows are compared by value: each row becomes a JSON array, so 10.5 equals 10.50, but a
-- text '10.5' does not equal the number. Column names are not compared.

CREATE SCHEMA dojo;

CREATE TABLE dojo.results (
    n       serial PRIMARY KEY,
    name    text NOT NULL,
    passed  boolean NOT NULL,
    message text
);

-- Test names become identifiers like the other images: 'Lists the top 3' -> Lists_the_top_3.
CREATE FUNCTION dojo.slug(test text) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
    SELECT trim(BOTH '_' FROM regexp_replace(test, '[^A-Za-z0-9_]+', '_', 'g'))
$$;

-- The SQL of a file of the kata, ready to run: trailing semicolons and comments removed.
-- A file with nothing but comments is the starter's placeholder.
CREATE FUNCTION dojo.sql_of(file text) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    body text;
    previous text;
BEGIN
    BEGIN
        body := pg_read_file(current_setting('dojo.dir') || '/' || file);
    EXCEPTION WHEN undefined_file THEN
        RAISE EXCEPTION 'the file % is missing: create it at the top of the kata folder', file;
    END;
    LOOP
        previous := body;
        body := regexp_replace(body, '\s+$', '');
        body := regexp_replace(body, ';[ \t]*--[^\n]*$', '');
        body := regexp_replace(body, ';$', '');
        body := regexp_replace(body, '(^|\n)[ \t]*--[^\n]*$', '');
        EXIT WHEN body = previous;
    END LOOP;
    IF body ~ '^\s*$' THEN
        RAISE EXCEPTION 'todo: write %', file;
    END IF;
    RETURN body;
END
$$;

-- Runs the statements of a file of the kata, for a setup that needs the learner's schema first:
--   setup => $$SELECT dojo.run_file('schema.sql'); INSERT INTO ...$$
CREATE FUNCTION dojo.run_file(file text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE dojo.sql_of(file);
END
$$;

-- Every row a query returns, as JSON arrays of its values in column order.
CREATE FUNCTION dojo.rows_of(query text) RETURNS jsonb[]
LANGUAGE plpgsql AS $$
DECLARE
    r record;
    result jsonb[] := '{}';
BEGIN
    FOR r IN EXECUTE query LOOP
        result := array_append(result, (
            SELECT coalesce(jsonb_agg(e.value ORDER BY e.ord), '[]'::jsonb)
            FROM json_each(to_json(r)) WITH ORDINALITY AS e(key, value, ord)
        ));
    END LOOP;
    RETURN result;
END
$$;

-- The EXPLAIN of a query, without costs, as one text: for checks such as LIKE '%Index Scan%'.
CREATE FUNCTION dojo.plan(query text) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    line text;
    result text := '';
BEGIN
    FOR line IN EXECUTE 'EXPLAIN (COSTS OFF) ' || query LOOP
        result := result || line || E'\n';
    END LOOP;
    RETURN result;
END
$$;

CREATE FUNCTION dojo.show(rows jsonb[]) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
    SELECT coalesce(string_agg(r::text, ', '), '(none)')
    FROM (SELECT r FROM unnest(rows) AS r LIMIT 3) AS first_rows
$$;

-- NULL when both lists hold the same rows, otherwise a message about the first difference.
CREATE FUNCTION dojo.compare(have jsonb[], want jsonb[], ordered boolean) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    missing jsonb[];
    unexpected jsonb[];
    message text;
BEGIN
    IF ordered THEN
        IF have = want THEN
            RETURN NULL;
        END IF;
        FOR i IN 1 .. greatest(cardinality(have), cardinality(want)) LOOP
            IF have[i] IS DISTINCT FROM want[i] THEN
                message := format('row %s: expected %s, got %s', i,
                                  coalesce(want[i]::text, 'no row'), coalesce(have[i]::text, 'no row'));
                EXIT;
            END IF;
        END LOOP;
        IF cardinality(have) <> cardinality(want) THEN
            message := format('expected %s rows, got %s', cardinality(want), cardinality(have))
                       || E'\n' || message;
        END IF;
        RETURN message;
    END IF;

    SELECT array_agg(v) INTO missing
    FROM (SELECT v FROM unnest(want) AS v EXCEPT ALL SELECT v FROM unnest(have) AS v) AS d;
    SELECT array_agg(v) INTO unexpected
    FROM (SELECT v FROM unnest(have) AS v EXCEPT ALL SELECT v FROM unnest(want) AS v) AS d;
    IF missing IS NULL AND unexpected IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN format('expected %s rows, got %s (%s missing, %s unexpected)',
                  cardinality(want), cardinality(have),
                  coalesce(cardinality(missing), 0), coalesce(cardinality(unexpected), 0))
           || CASE WHEN missing IS NULL THEN '' ELSE E'\nmissing: ' || dojo.show(missing) END
           || CASE WHEN unexpected IS NULL THEN '' ELSE E'\nunexpected: ' || dojo.show(unexpected) END;
END
$$;

-- Runs one test and returns NULL when it passed, otherwise why it failed. Always rolls back.
CREATE FUNCTION dojo.attempt(kind text, file text, query text, want text, ordered boolean,
                             expected_state text, why text, setup text, after text) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    stage text := 'the test setup';
    learner text;
    failure text;
    have_rows jsonb[];
    checked boolean;
    state text;
BEGIN
    BEGIN
        IF setup IS NOT NULL THEN
            EXECUTE setup;
        END IF;
        IF file IS NOT NULL THEN
            stage := file;
            learner := dojo.sql_of(file);
        END IF;
        IF kind = 'query_eq' THEN
            have_rows := dojo.rows_of(learner);
            stage := 'the test';
            failure := dojo.compare(have_rows, dojo.rows_of(want), ordered);
        ELSE
            IF learner IS NOT NULL THEN
                EXECUTE learner;
            END IF;
            stage := 'the test';
            IF after IS NOT NULL THEN
                EXECUTE after;
            END IF;
            IF kind = 'script_eq' THEN
                failure := dojo.compare(dojo.rows_of(query), dojo.rows_of(want), ordered);
            ELSIF kind = 'script_ok' THEN
                EXECUTE query INTO checked;
                IF checked IS NOT TRUE THEN
                    failure := why;
                END IF;
            ELSIF kind = 'script_fails' THEN
                BEGIN
                    EXECUTE query;
                    failure := why || E'\nbut this was accepted: ' || query;
                EXCEPTION WHEN OTHERS THEN
                    state := SQLSTATE;
                    -- '23505' matches that code only; '23' the whole class.
                    IF state NOT LIKE expected_state || '%' THEN
                        failure := format(E'%s\nit failed, but with %s (SQLSTATE %s, expected %s)',
                                          why, SQLERRM, state, expected_state);
                    END IF;
                END;
            END IF;
        END IF;
        RAISE EXCEPTION USING ERRCODE = 'DJ000', MESSAGE = coalesce(failure, 'passed');
    EXCEPTION
        WHEN SQLSTATE 'DJ000' THEN
            failure := nullif(SQLERRM, 'passed');
        WHEN query_canceled THEN
            failure := format('%s ran for more than 15 s: look for a missing join condition or a recursion that never ends', stage);
        WHEN OTHERS THEN
            IF SQLERRM LIKE 'todo:%' OR SQLERRM LIKE 'the file % is missing%' THEN
                failure := SQLERRM;
            ELSE
                failure := format('%s failed: %s (SQLSTATE %s)', stage, SQLERRM, SQLSTATE);
            END IF;
    END;
    RETURN failure;
END
$$;

CREATE FUNCTION dojo.record(test text, failure text) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO dojo.results (name, passed, message)
    VALUES (dojo.slug(test), failure IS NULL, regexp_replace(failure, '\n\s*\n', E'\n', 'g'));
    RETURN CASE WHEN failure IS NULL THEN 'ok ' ELSE 'FAILED ' END || dojo.slug(test);
END
$$;

-- The rows of `file` (a query) must equal `want` (any query, usually VALUES).
CREATE FUNCTION dojo.query_eq(test text, file text, want text, ordered boolean DEFAULT false,
                              setup text DEFAULT NULL) RETURNS text
LANGUAGE sql AS $$
    SELECT dojo.record(test, dojo.attempt('query_eq', file, NULL, want, ordered, NULL, NULL, setup, NULL))
$$;

-- After running `file` (statements), the rows of `query` must equal `want`.
CREATE FUNCTION dojo.script_eq(test text, file text, query text, want text,
                               ordered boolean DEFAULT false, setup text DEFAULT NULL,
                               after text DEFAULT NULL) RETURNS text
LANGUAGE sql AS $$
    SELECT dojo.record(test, dojo.attempt('script_eq', file, query, want, ordered, NULL, NULL, setup, after))
$$;

-- After running `file`, `statement` must fail with an SQLSTATE that starts with `state`.
CREATE FUNCTION dojo.script_fails(test text, file text, statement text, state text, why text,
                                  setup text DEFAULT NULL, after text DEFAULT NULL) RETURNS text
LANGUAGE sql AS $$
    SELECT dojo.record(test, dojo.attempt('script_fails', file, statement, NULL, NULL, state, why, setup, after))
$$;

-- After running `file` (NULL for none), `condition` must return true.
CREATE FUNCTION dojo.script_ok(test text, file text, condition text, why text,
                               setup text DEFAULT NULL, after text DEFAULT NULL) RETURNS text
LANGUAGE sql AS $$
    SELECT dojo.record(test, dojo.attempt('script_ok', file, condition, NULL, NULL, NULL, why, setup, after))
$$;

-- Every result in the `cargo test` format the dojo parses.
CREATE FUNCTION dojo.report() RETURNS text
LANGUAGE sql AS $$
    SELECT CASE WHEN count(*) = 0 THEN 'error: no tests ran' ELSE
        format(E'\nrunning %s tests\n', count(*))
        || string_agg(format('test %s ... %s', name, CASE WHEN passed THEN 'ok' ELSE 'FAILED' END), E'\n' ORDER BY n)
        || E'\n'
        || CASE WHEN bool_and(passed) THEN format(E'\ntest result: ok. %s passed; 0 failed', count(*))
           ELSE E'\nfailures:\n\n'
                || (SELECT string_agg(format(E'---- %s stdout ----\nthread ''%s'' panicked at %s:\n%s\n',
                                             f.name, f.name, f.name, f.message), E'\n' ORDER BY f.n)
                    FROM dojo.results AS f WHERE NOT f.passed)
                || format(E'\nfailures:\n\ntest result: FAILED. %s passed; %s failed',
                          count(*) FILTER (WHERE passed), count(*) FILTER (WHERE NOT passed))
           END
    END
    FROM dojo.results
$$;
