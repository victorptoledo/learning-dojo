#!/bin/sh
# Migrates the schema before serving (unless MIGRATE_ON_START=false, when a separate job
# does it), then replaces itself with the catalog, so the server is PID 1 and gets SIGTERM.
set -eu

if [ "${1:-}" = serve ] && [ "${MIGRATE_ON_START:-true}" = true ]; then
    catalog migrate
fi

exec catalog "$@"
