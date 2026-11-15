# Sourced by check.sh and play.sh: starts PostgreSQL on a Unix socket in /tmp (no TCP),
# creates the `dojo` database and loads the kata's fixture/*.sql in name order.

start_server() {
    if ! pg_ctl --wait --silent --log=/tmp/postgres.log start \
        -o "-c listen_addresses='' -c unix_socket_directories=/tmp -c fsync=off \
            -c synchronous_commit=off -c full_page_writes=off -c shared_buffers=64MB \
            -c max_parallel_workers_per_gather=0 -c jit=off -c TimeZone=UTC" > /dev/null; then
        echo "error: PostgreSQL did not start"
        cat /tmp/postgres.log
        exit 1
    fi
    createdb --template=template0 dojo
}

# load_fixture DIR: runs DIR/fixture/*.sql in the `dojo` database, stopping at the first error.
load_fixture() {
    local file
    for file in "$1"/fixture/*.sql; do
        [ -e "$file" ] || continue
        if ! psql -X -q -v ON_ERROR_STOP=1 -d dojo -f "$file" > /tmp/fixture.log 2>&1; then
            echo "error: the fixture ${file#"$1"/} does not load"
            cat /tmp/fixture.log
            exit 1
        fi
    done
    psql -X -q -d dojo -c "ANALYZE" > /dev/null
}
