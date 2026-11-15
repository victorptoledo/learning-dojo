#!/bin/bash
# Checks a SQL kata in /work: the learner's queries are the .sql files at the top of the folder,
# fixture/*.sql builds the tables, and the tests in tests/*.sql (visible ones from the starter,
# grading ones staged next to them) call the dojo schema. Prints the result in `cargo test` format.
set -u
# shellcheck source=pg.sh
. /opt/dojo/pg.sh

kata=/work
if ! ls "$kata"/tests/*.sql > /dev/null 2>&1; then
    echo "error: no tests found in tests/"
    exit 1
fi

start_server
load_fixture "$kata"

if ! psql -X -q -v ON_ERROR_STOP=1 -d dojo -f /opt/dojo/dojo.sql > /tmp/dojo.log 2>&1; then
    echo "error: the dojo test schema does not load"
    cat /tmp/dojo.log
    exit 1
fi
# The user is called dojo, so the default search_path ("$user", public) would put the
# learner's tables in the dojo schema. Keep them in public.
psql -X -q -d dojo -c "ALTER DATABASE dojo SET dojo.dir = '$kata'"     -c "ALTER DATABASE dojo SET search_path = public" > /dev/null

# Each test is one statement; every check runs inside a block that is rolled back, so tests
# never see each other's changes. A test that runs too long is stopped and reported.
# The visible tests first, so their results lead the report.
mapfile -t files < <(ls "$kata"/tests/visible.sql 2> /dev/null; ls "$kata"/tests/*.sql | grep -v '/visible\.sql$')
for file in "${files[@]}"; do
    if ! PGOPTIONS="-c statement_timeout=15s" psql -X -q -v ON_ERROR_STOP=1 -d dojo \
        -f "$file" > /tmp/tests.log 2>&1; then
        echo "error: the test file ${file#"$kata"/} does not run"
        cat /tmp/tests.log
        exit 1
    fi
done

psql -X -q -A -t -d dojo -c "SELECT dojo.report()"
psql -X -q -A -t -d dojo -c "SELECT bool_and(passed) AND count(*) > 0 FROM dojo.results" | grep -qx t
